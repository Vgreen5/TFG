package modelo;

public class Grupo {
    private String codigo;
    private String nombre;
    private boolean ensenyanza;
    private boolean finia;
    private String tutorId; 
	public String getCodigo() {
		return codigo;
	}
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public boolean isEnsenyanza() {
		return ensenyanza;
	}
	public void setEnsenyanza(boolean ensenyanza) {
		this.ensenyanza = ensenyanza;
	}
	public String getTutorId() {
		return tutorId;
	}
	public void setTutorId(String tutorId) {
		this.tutorId = tutorId;
	}
	public boolean isFinia() {
		return finia;
	}
	public void setFinia(boolean finia) {
		this.finia = finia;
	}

    // Constructor, getters y setters
}