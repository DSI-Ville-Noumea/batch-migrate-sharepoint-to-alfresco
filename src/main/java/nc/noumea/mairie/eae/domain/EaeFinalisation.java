package nc.noumea.mairie.eae.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "EAE_FINALISATION")
@PersistenceUnit(unitName = "eaePersistenceUnit")
@NamedQueries ({
	@NamedQuery(name = "getListEaeFinalisation", query = "select e from EaeFinalisation e where e.nodeRefAlfresco is NULL order by idEaeFinalisation ")
})
public class EaeFinalisation {

	@Id
	@Column(name = "ID_EAE_FINALISATION")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idEaeFinalisation;

	@NotNull
	@Column(name = "DATE_FINALISATION")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateFinalisation;

	@NotNull
	@Column(name = "ID_AGENT")
	private int idAgent;

	@NotNull
	@Column(name = "ID_GED_DOCUMENT")
	private String idGedDocument;

	@NotNull
	@Column(name = "VERSION_GED_DOCUMENT")
	private String versionGedDocument;

	@Column(name = "COMMENTAIRE")
	private String commentaire;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ID_EAE")
	private Eae eae;

	@Column(name = "NODE_REF_ALFRESCO")
	private String nodeRefAlfresco;

	@Column(name = "COMMENTAIRE_ALFRESCO")
	private String commentaireAlfresco;


	public Integer getIdEaeFinalisation() {
		return idEaeFinalisation;
	}

	public void setIdEaeFinalisation(Integer idEaeFinalisation) {
		this.idEaeFinalisation = idEaeFinalisation;
	}

	public Date getDateFinalisation() {
		return dateFinalisation;
	}

	public void setDateFinalisation(Date dateFinalisation) {
		this.dateFinalisation = dateFinalisation;
	}

	public int getIdAgent() {
		return idAgent;
	}

	public void setIdAgent(int idAgent) {
		this.idAgent = idAgent;
	}

	public String getIdGedDocument() {
		return idGedDocument;
	}

	public void setIdGedDocument(String idGedDocument) {
		this.idGedDocument = idGedDocument;
	}

	public String getVersionGedDocument() {
		return versionGedDocument;
	}

	public void setVersionGedDocument(String versionGedDocument) {
		this.versionGedDocument = versionGedDocument;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	public Eae getEae() {
		return eae;
	}

	public void setEae(Eae eae) {
		this.eae = eae;
	}

	public String getNodeRefAlfresco() {
		return nodeRefAlfresco;
	}

	public void setNodeRefAlfresco(String nodeRefAlfresco) {
		this.nodeRefAlfresco = nodeRefAlfresco;
	}

	public String getCommentaireAlfresco() {
		return commentaireAlfresco;
	}

	public void setCommentaireAlfresco(String commentaireAlfresco) {
		if(commentaireAlfresco!=null && commentaireAlfresco.length()>200){
			this.commentaireAlfresco = commentaireAlfresco.substring(0, 200);			
		}else{
			this.commentaireAlfresco = commentaireAlfresco;
		}
	}
	
}
