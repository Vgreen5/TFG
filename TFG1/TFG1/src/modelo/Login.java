package modelo;

public class Login {
    private String docenteId;
    private String mail;
    private boolean esAdmin;
    private String password;
    public Login(String docenteId, String password, Boolean esAdmin) {
        this.docenteId = docenteId;
        this.esAdmin = esAdmin;
        this.password = password;
    }

    public String getDocenteId() { return docenteId; }
    public String getMail() { return mail; }
    public boolean isEsAdmin() { return esAdmin; }

    public void setEsAdmin(boolean esAdmin) {
        this.esAdmin = esAdmin;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
