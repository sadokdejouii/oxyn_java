import json
import os
import re
import sys
import unicodedata
import warnings
from pathlib import Path

os.environ.setdefault("HF_HUB_DISABLE_PROGRESS_BARS", "1")
os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")

import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from transformers import pipeline

warnings.filterwarnings("ignore")

MODEL_DIR = Path(__file__).resolve().parent / "model_cache"
MODEL_FILE = MODEL_DIR / "avis_toxicity_model.joblib"
PRETRAINED_MODEL_NAME = os.environ.get(
    "OXYN_TOXIC_MODEL",
    "citizenlab/distilbert-base-multilingual-cased-toxicity",
)
BLOCK_THRESHOLD = 0.74
_hf_pipeline = None

STRONG_TERMS = {
    "connard", "connasse", "encule", "enculer", "fdp", "pute", "salope",
    "batard", "tapette", "bougnoule", "nigger", "negro", "racaille", "kys"
}

BLOCKED_PHRASES = [
    "sale arabe",
    "sale noir",
    "sale juif",
    "sale race",
    "nigga",
    "retourne dans ton pays",
    "retourne chez toi",
    "va mourir",
    "va crever",
    "je vais te tuer",
    "kill yourself",
]

TOXIC_SAMPLES = [
    "sale arabe retourne dans ton pays",
    "sale noir degage d ici",
    "sale africain on ne veut pas de toi",
    "ce service est pour les connards",
    "tu es une salope incompetente",
    "encule va mourir",
    "va crever espece de racaille",
    "nigga get out of here",
    "kill yourself idiot",
    "gros batard ton evenement est nul",
    "bougnoule retourne chez toi",
    "negro get out",
    "je vais te tuer pauvre con",
    "bande de cons racistes",
    "cette equipe est composee de debiles",
    "organisateurs de merde",
    "tu pues la merde sale type",
    "gros connard incompetent",
    "sale race de chien",
    "je deteste ces gens sales",
    "team de putes et d incapables",
    "cet evenement pue la merde",
    "ta gueule pauvre idiot",
    "degage on veut pas de toi",
]

SAFE_SAMPLES = [
    "tres bon evenement bien organise",
    "ambiance positive et equipe serieuse",
    "j ai adore la seance merci beaucoup",
    "coach sympathique et professionnel",
    "experience correcte mais perfectible",
    "bonne organisation generale",
    "les horaires etaient respectes",
    "super accueil et salle tres propre",
    "je recommande cet evenement",
    "bon rapport qualite prix",
    "l activite etait interessante et bien animee",
    "equipe souriante et disponible",
    "avis positif dans l ensemble",
    "un peu de retard mais rien de grave",
    "j aimerais plus de places la prochaine fois",
    "merci pour l encadrement et le suivi",
    "evenement utile et motivant",
    "bonne experience avec quelques details a ameliorer",
    "j ai passe un bon moment",
    "tres satisfait du service",
    "la securite etait bien geree",
    "explications claires et seance dynamique",
    "peut mieux faire au niveau du son",
    "globalement je suis content de l organisation",
]

IDENTITY_TERMS = {
    "arabe", "africain", "africaine", "noir", "noire", "juif", "juive",
    "musulman", "musulmane", "etranger", "immigre", "immigres", "race",
}

ATTACK_TERMS = {
    "sale", "degage", "dehors", "honte", "merde", "pourri", "poubelle",
    "degoutant", "nul", "haine", "deteste",
}

THREAT_TERMS = {
    "tuer", "frapper", "crever", "mourir", "detruire", "egorger", "massacrer",
}

SECOND_PERSON_TERMS = {"tu", "toi", "ton", "ta", "tes", "vous", "votre", "vos", "your"}

PROFANITY_TERMS = STRONG_TERMS | {"merde", "con", "conne", "idiot", "idiote", "debile", "debiles"}


def normalize_text(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text or "")
    normalized = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    normalized = normalized.lower()
    normalized = (
        normalized.replace("0", "o")
        .replace("1", "i")
        .replace("3", "e")
        .replace("4", "a")
        .replace("5", "s")
        .replace("7", "t")
        .replace("@", "a")
        .replace("$", "s")
    )
    normalized = re.sub(r"[^a-z0-9\s]", " ", normalized)
    normalized = re.sub(r"(.)\1{2,}", r"\1\1", normalized)
    normalized = re.sub(r"\s+", " ", normalized).strip()
    return normalized


def squash_text(text: str) -> str:
    return normalize_text(text).replace(" ", "")


def contains_signal(term: str, padded_text: str, squashed_text: str) -> bool:
    normalized_term = normalize_text(term)
    return f" {normalized_term} " in padded_text or normalized_term.replace(" ", "") in squashed_text


def find_hits(terms: set[str], padded_text: str, squashed_text: str) -> list[str]:
    return sorted(term for term in terms if contains_signal(term, padded_text, squashed_text))


def compute_uppercase_ratio(text: str) -> float:
    letters = [char for char in text if char.isalpha()]
    if not letters:
        return 0.0
    uppercase = [char for char in letters if char.isupper()]
    return len(uppercase) / len(letters)


def build_rule_signals(cleaned_comment: str, normalized_text: str) -> dict:
    padded_text = f" {normalized_text} "
    squashed = normalized_text.replace(" ", "")

    profanity_hits = find_hits(PROFANITY_TERMS, padded_text, squashed)
    identity_hits = find_hits(IDENTITY_TERMS, padded_text, squashed)
    attack_hits = find_hits(ATTACK_TERMS, padded_text, squashed)
    threat_hits = find_hits(THREAT_TERMS, padded_text, squashed)
    second_person_hits = find_hits(SECOND_PERSON_TERMS, padded_text, squashed)
    phrase_hits = [phrase for phrase in BLOCKED_PHRASES if contains_signal(phrase, padded_text, squashed)]

    scores = {"toxicity": 0.0, "insult": 0.0, "hate": 0.0, "threat": 0.0}
    reasons: list[str] = []

    if profanity_hits:
        scores["insult"] = max(scores["insult"], 0.64 + min(0.18, len(profanity_hits) * 0.05))
        scores["toxicity"] = max(scores["toxicity"], 0.58 + min(0.14, len(profanity_hits) * 0.04))

    if threat_hits:
        scores["threat"] = max(scores["threat"], 0.78 + min(0.12, len(threat_hits) * 0.04))
        scores["toxicity"] = max(scores["toxicity"], 0.74)

    if phrase_hits:
        scores["hate"] = max(scores["hate"], 0.9 + min(0.06, len(phrase_hits) * 0.02))

    if identity_hits and (attack_hits or profanity_hits or threat_hits or phrase_hits):
        scores["hate"] = max(scores["hate"], 0.88)

    if attack_hits and second_person_hits:
        scores["insult"] = max(scores["insult"], 0.68)

    if compute_uppercase_ratio(cleaned_comment) >= 0.55 and (profanity_hits or threat_hits or phrase_hits):
        scores["toxicity"] = min(0.97, max(scores["toxicity"], 0.7) + 0.05)

    if any(signal in cleaned_comment for signal in ("!!", "??", "?!", "!?")) and (profanity_hits or threat_hits):
        scores["toxicity"] = min(0.97, max(scores["toxicity"], 0.72) + 0.04)

    if scores["hate"] >= 0.82:
        reasons.append("attaque visant une identite, une origine ou une religion")
    if scores["threat"] >= 0.78:
        reasons.append("menace ou appel a la violence")
    if scores["insult"] >= 0.64:
        reasons.append("insultes ou langage humiliant")
    if not reasons and max(scores.values()) >= 0.62:
        reasons.append("ton agressif ou toxique")

    primary_category = max(scores, key=scores.get)
    primary_score = scores[primary_category]

    return {
        "scores": scores,
        "primaryCategory": primary_category,
        "primaryScore": primary_score,
        "reasons": reasons,
        "forceBlock": scores["hate"] >= 0.88 or scores["threat"] >= 0.82,
    }


def build_pipeline() -> Pipeline:
    samples = [normalize_text(item) for item in SAFE_SAMPLES + TOXIC_SAMPLES]
    labels = [0] * len(SAFE_SAMPLES) + [1] * len(TOXIC_SAMPLES)

    pipeline = Pipeline(
        [
            ("tfidf", TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5))),
            ("classifier", LogisticRegression(max_iter=4000, class_weight="balanced", random_state=42)),
        ]
    )
    pipeline.fit(samples, labels)
    return pipeline


def load_pipeline() -> Pipeline:
    if MODEL_FILE.exists():
        return joblib.load(MODEL_FILE)

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    pipeline = build_pipeline()
    joblib.dump(pipeline, MODEL_FILE)
    return pipeline


def load_pretrained_pipeline():
    global _hf_pipeline
    if _hf_pipeline is None:
        _hf_pipeline = pipeline(
            "text-classification",
            model=PRETRAINED_MODEL_NAME,
            tokenizer=PRETRAINED_MODEL_NAME,
        )
    return _hf_pipeline


def warm_up_models():
    try:
        load_pretrained_pipeline()
        return PRETRAINED_MODEL_NAME
    except Exception:
        load_pipeline()
        return "local-fallback"


def keyword_boost(score: float, normalized_text: str) -> float:
    padded = f" {normalized_text} "
    term_hits = sum(1 for term in STRONG_TERMS if f" {term} " in padded)
    phrase_hits = sum(1 for phrase in BLOCKED_PHRASES if phrase in normalized_text)

    boosted_score = score
    if term_hits:
        boosted_score = max(boosted_score, 0.72 + min(0.18, term_hits * 0.08))
    if phrase_hits:
        boosted_score = max(boosted_score, 0.88 + min(0.10, phrase_hits * 0.03))

    return min(boosted_score, 0.99)


def recommendation_for_category(category: str) -> str:
    if category == "hate":
        return "Reformulez votre avis sans attaque contre une origine, une religion ou une identite."
    if category == "threat":
        return "Supprimez toute menace ou appel a la violence et gardez une critique factuelle."
    if category == "insult":
        return "Expliquez le probleme de maniere precise et polie, sans insulte."
    return "Decrivez les points a ameliorer avec des faits concrets et un ton respectueux."


def user_message_for_category(category: str) -> str:
    if category == "hate":
        return "Votre commentaire a ete refuse car il contient un contenu haineux ou discriminatoire."
    if category == "threat":
        return "Votre commentaire a ete refuse car il contient une menace ou un appel a la violence."
    if category == "insult":
        return "Votre commentaire a ete refuse car il contient des insultes ou un langage humiliant."
    return "Votre commentaire a ete refuse car il contient un contenu offensant, haineux ou inapproprie."


def predict_with_pretrained_model(cleaned_comment: str, normalized_text: str):
    classifier = load_pretrained_pipeline()
    prediction = classifier(cleaned_comment, truncation=True, max_length=512)[0]
    label = str(prediction.get("label", "")).lower()
    score = float(prediction.get("score", 0.0))

    if "not_toxic" in label or label == "non_toxic":
        toxicity_score = 1.0 - score
    else:
        toxicity_score = score

    return {
        "toxicityScore": round(keyword_boost(toxicity_score, normalized_text), 4),
        "provider": PRETRAINED_MODEL_NAME,
    }


def predict_with_local_signal(normalized_text: str):
    local_pipeline = load_pipeline()
    probability = float(local_pipeline.predict_proba([normalized_text])[0][1])
    return {
        "toxicityScore": round(keyword_boost(probability, normalized_text), 4),
    }


def classify_comment(comment: str) -> dict:
    cleaned_comment = re.sub(r"\s+", " ", (comment or "").strip())
    if not cleaned_comment:
        return {
            "cleanedComment": "",
            "toxicityScore": 0.0,
            "blocked": False,
            "userMessage": "",
        }

    normalized_text = normalize_text(cleaned_comment)
    rule_signals = build_rule_signals(cleaned_comment, normalized_text)
    local_signal = predict_with_local_signal(normalized_text)

    try:
        pretrained_signal = predict_with_pretrained_model(cleaned_comment, normalized_text)
        provider = f"{pretrained_signal['provider']} + local-rules"
        model_score = pretrained_signal["toxicityScore"]
    except Exception:
        pretrained_signal = None
        provider = "local-fallback + local-rules"
        model_score = local_signal["toxicityScore"]

    local_score = local_signal["toxicityScore"]
    rule_score = rule_signals["primaryScore"]
    combined_score = max(model_score, min(0.95, local_score * 0.96 + 0.02), rule_score)

    if rule_score >= 0.62 and max(model_score, local_score) >= 0.5:
        combined_score = max(combined_score, min(0.97, max(model_score, local_score) + 0.14))

    primary_category = rule_signals["primaryCategory"] if rule_score >= 0.62 else "toxicity"
    blocked = rule_signals["forceBlock"] or combined_score >= BLOCK_THRESHOLD
    reasons = list(rule_signals["reasons"])
    if blocked and not reasons:
        reasons.append("score de toxicite eleve detecte par la moderation")

    user_message = user_message_for_category(primary_category) if blocked else ""
    recommendation = recommendation_for_category(primary_category) if blocked else ""

    return {
        "cleanedComment": cleaned_comment,
        "toxicityScore": round(combined_score, 4),
        "blocked": blocked,
        "userMessage": user_message,
        "provider": provider,
        "primaryCategory": primary_category,
        "reasons": reasons,
        "recommendation": recommendation,
    }


def main() -> int:
    comment = sys.stdin.read()
    result = classify_comment(comment)
    sys.stdout.write(json.dumps(result, ensure_ascii=False))
    return 0


def serve() -> int:
    provider = warm_up_models()
    sys.stdout.write(json.dumps({"status": "ready", "provider": provider}, ensure_ascii=False) + "\n")
    sys.stdout.flush()

    for line in sys.stdin:
        request_line = line.strip()
        if not request_line:
            continue

        try:
            payload = json.loads(request_line)
            comment = payload.get("comment", "")
            result = classify_comment(comment)
        except Exception as exc:
            result = {"error": str(exc)}

        sys.stdout.write(json.dumps(result, ensure_ascii=False) + "\n")
        sys.stdout.flush()

    return 0


if __name__ == "__main__":
    try:
        if len(sys.argv) > 1 and sys.argv[1] == "--serve":
            raise SystemExit(serve())
        raise SystemExit(main())
    except Exception as exc:
        sys.stdout.write(json.dumps({"error": str(exc)}))
        raise SystemExit(1)