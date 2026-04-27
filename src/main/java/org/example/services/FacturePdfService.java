package org.example.services;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.example.entities.User;
import org.example.entities.LigneCommandeAffichage;
import org.example.entities.commandes;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;

public class FacturePdfService {

    public void genererFacture(FileOutputStream out, commandes commande, List<LigneCommandeAffichage> lignes)
            throws DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        BaseColor brandBlue = new BaseColor(6, 41, 108);
        BaseColor borderLight = new BaseColor(226, 231, 239);
        BaseColor textDark = new BaseColor(34, 44, 60);
        BaseColor muted = new BaseColor(104, 116, 133);
        BaseColor lightBox = new BaseColor(245, 247, 251);

        Font logoFont = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, brandBlue);
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, brandBlue);
        Font statusFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, textDark);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, brandBlue);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, textDark);
        Font metaFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, muted);
        Font totalLabelFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, brandBlue);
        Font totalValueFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, brandBlue);

        User currentUser = SessionContext.getInstance().getCurrentUser();
        String clientNom = currentUser != null ? safe(currentUser.getFullName()).trim() : "Client";
        String clientEmail = currentUser != null ? safe(currentUser.getEmail()) : "-";
        String clientTel = currentUser != null ? safe(currentUser.getTelephone()) : "-";

        PdfPTable top = new PdfPTable(new float[]{1f, 1f});
        top.setWidthPercentage(100f);
        top.setSpacingAfter(8f);
        PdfPCell leftTop = new PdfPCell();
        leftTop.setBorder(PdfPCell.NO_BORDER);
        leftTop.addElement(new Paragraph("OXYN", logoFont));
        leftTop.addElement(new Paragraph("Votre partenaire fitness", metaFont));

        PdfPCell rightTop = new PdfPCell();
        rightTop.setBorder(PdfPCell.NO_BORDER);
        Paragraph factureTitle = new Paragraph("FACTURE", titleFont);
        factureTitle.setAlignment(Element.ALIGN_RIGHT);
        rightTop.addElement(factureTitle);
        Paragraph dateP = new Paragraph("Date: " + safe(commande.getDate_commande()), valueFont);
        dateP.setAlignment(Element.ALIGN_RIGHT);
        rightTop.addElement(dateP);
        Paragraph statutP = new Paragraph(safe(commande.getStatut_commande()).toUpperCase(Locale.ROOT), statusFont);
        statutP.setAlignment(Element.ALIGN_RIGHT);
        rightTop.addElement(statutP);

        top.addCell(leftTop);
        top.addCell(rightTop);
        document.add(top);

        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100f);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setFixedHeight(1.8f);
        separatorCell.setBorder(PdfPCell.NO_BORDER);
        separatorCell.setBackgroundColor(brandBlue);
        separator.addCell(separatorCell);
        separator.setSpacingAfter(12f);
        document.add(separator);

        document.add(sectionTitle("OXYN", labelFont));
        document.add(infoText("Email: contact@oxyn.com", valueFont));
        document.add(infoText("Telephone: +216 5X-XXXXXX", valueFont));
        document.add(infoText("Adresse: Tunisie", valueFont));
        document.add(new Paragraph(" "));

        document.add(sectionTitle("Informations Client", labelFont));
        document.add(infoText(clientNom.isBlank() ? "Client" : clientNom, valueFont));
        document.add(infoText("Email: " + clientEmail, valueFont));
        document.add(infoText("Telephone: " + clientTel, valueFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{5f, 1.5f, 1.5f, 2f});
        table.setWidthPercentage(100f);
        table.setSpacingAfter(10f);
        addHeader(table, "Produit", brandBlue, borderLight);
        addHeader(table, "Prix Unitaire", brandBlue, borderLight);
        addHeader(table, "Quantite", brandBlue, borderLight);
        addHeader(table, "Total", brandBlue, borderLight);

        if (lignes != null) {
            for (LigneCommandeAffichage ligne : lignes) {
                table.addCell(detailCell(safe(ligne.getNomProduit()), borderLight, valueFont));
                table.addCell(detailCell(String.format(Locale.FRENCH, "%.2f DH", ligne.getPrixUnitaire()), borderLight, valueFont));
                table.addCell(detailCell(String.valueOf(ligne.getQuantite()), borderLight, valueFont));
                table.addCell(detailCell(String.format(Locale.FRENCH, "%.2f DH", ligne.getSousTotal()), borderLight, valueFont));
            }
        }
        document.add(table);

        PdfPTable modePaiement = new PdfPTable(1);
        modePaiement.setWidthPercentage(100f);
        PdfPCell modeCell = new PdfPCell(new Paragraph("Mode de paiement: " + safe(commande.getMode_paiement_commande()), valueFont));
        modeCell.setBorderColor(borderLight);
        modeCell.setBackgroundColor(lightBox);
        modeCell.setPadding(10f);
        modePaiement.addCell(modeCell);
        modePaiement.setSpacingAfter(12f);
        document.add(modePaiement);

        double sousTotal = commande.getTotal_commande();
        double tva = 0d;
        double totalPayer = sousTotal + tva;

        PdfPTable totals = new PdfPTable(new float[]{1f, 0.35f});
        totals.setWidthPercentage(100f);
        PdfPCell empty = new PdfPCell();
        empty.setBorder(PdfPCell.NO_BORDER);
        totals.addCell(empty);

        PdfPCell amountBox = new PdfPCell();
        amountBox.setBorder(PdfPCell.NO_BORDER);
        amountBox.addElement(rightLine("Sous-total:", String.format(Locale.FRENCH, "%.2f DH", sousTotal), valueFont));
        amountBox.addElement(rightLine("TVA (0%):", String.format(Locale.FRENCH, "%.2f DH", tva), valueFont));
        amountBox.addElement(new Paragraph(" "));
        amountBox.addElement(rightLine("Total a payer:", String.format(Locale.FRENCH, "%.2f DH", totalPayer), totalLabelFont, totalValueFont));
        totals.addCell(amountBox);
        document.add(totals);

        document.close();
    }

    private static Paragraph sectionTitle(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(4f);
        return p;
    }

    private static Paragraph infoText(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(2f);
        return p;
    }

    private static void addHeader(PdfPTable table, String text, BaseColor bg, BaseColor border) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE)));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(7f);
        cell.setBorderColor(border);
        table.addCell(cell);
    }

    private static PdfPCell detailCell(String text, BaseColor border, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(6f);
        cell.setBorderColor(border);
        return cell;
    }

    private static Paragraph rightLine(String label, String amount, Font font) {
        return rightLine(label, amount, font, font);
    }

    private static Paragraph rightLine(String label, String amount, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Paragraph(label, labelFont));
        Paragraph v = new Paragraph(amount, valueFont);
        v.setAlignment(Element.ALIGN_RIGHT);
        p.add(v);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }
}
