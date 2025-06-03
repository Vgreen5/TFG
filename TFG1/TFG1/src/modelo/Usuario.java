package modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Usuario {
    // Campos de la tabla docent
    private String document;
    private String nom;
    private String cognom1;
    private String cognom2;
    private String tipoDoc;
    private String sexe;
    private LocalDate dataIngres;
    private String horesLloc;
    private String horesDedicades;
    private LocalDate dataNaix;
    private boolean ensenyament;
    private boolean organisme;

    
    private String mail;
    private boolean esAdmin;

  
    public Usuario() {}

 
    public Usuario(String document, String nom, String cognom1, String cognom2,
                   String tipoDoc, String sexe, String dataIngres,
                   String horesLloc, String horesDedicades, String dataNaix,
                   boolean ensenyament, boolean organisme,
                   String mail, boolean esAdmin) {

        this.document = document;
        this.nom = nom;
        this.cognom1 = cognom1;
        this.cognom2 = cognom2;
        this.tipoDoc = tipoDoc;
        this.sexe = sexe;
        this.dataIngres = parseFecha(dataIngres);
        this.horesLloc = horesLloc;
        this.horesDedicades = horesDedicades;
        this.dataNaix = parseFecha(dataNaix);
        this.ensenyament = ensenyament;
        this.organisme = organisme;
        this.mail = mail;
        this.esAdmin = esAdmin;
    }

    
    private LocalDate parseFecha(String fecha) {
        if (fecha == null || fecha.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

   
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCognom1() { return cognom1; }
    public void setCognom1(String cognom1) { this.cognom1 = cognom1; }

    public String getCognom2() { return cognom2; }
    public void setCognom2(String cognom2) { this.cognom2 = cognom2; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public LocalDate getDataIngres() { return dataIngres; }
    public void setDataIngres(String dataIngres) { this.dataIngres = parseFecha(dataIngres); }

    public String getHoresLloc() { return horesLloc; }
    public void setHoresLloc(String horesLloc) { this.horesLloc = horesLloc; }

    public String getHoresDedicades() { return horesDedicades; }
    public void setHoresDedicades(String horesDedicades) { this.horesDedicades = horesDedicades; }

    public LocalDate getDataNaix() { return dataNaix; }
    public void setDataNaix(String dataNaix) { this.dataNaix = parseFecha(dataNaix); }

    public boolean isEnsenyament() { return ensenyament; }
    public void setEnsenyament(boolean ensenyament) { this.ensenyament = ensenyament; }

    public boolean isOrganisme() { return organisme; }
    public void setOrganisme(boolean organisme) { this.organisme = organisme; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public boolean isEsAdmin() { return esAdmin; }
    public void setAdmin(boolean esAdmin) { this.esAdmin = esAdmin; }

    public String getNombreCompleto() {
        return nom + " " + cognom1 + (cognom2 != null && !cognom2.isEmpty() ? " " + cognom2 : "");
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "document='" + document + '\'' +
                ", nombre='" + getNombreCompleto() + '\'' +
                ", mail='" + mail + '\'' +
                ", esAdmin=" + esAdmin +
                '}';
    }
}
