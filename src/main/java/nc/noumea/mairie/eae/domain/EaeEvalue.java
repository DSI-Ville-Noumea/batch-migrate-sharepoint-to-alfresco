package nc.noumea.mairie.eae.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

@Entity
@Table(name = "EAE_EVALUE")
@PersistenceUnit(unitName = "eaePersistenceUnit")
public class EaeEvalue {

	@Id
	@Column(name = "ID_EAE_EVALUE")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idEaeEvalue;

	@Column(name = "ID_AGENT")
	private int idAgent;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ID_EAE")
	private Eae eae;

	public Integer getIdEaeEvalue() {
		return idEaeEvalue;
	}

	public void setIdEaeEvalue(Integer idEaeEvalue) {
		this.idEaeEvalue = idEaeEvalue;
	}

	public int getIdAgent() {
		return idAgent;
	}

	public void setIdAgent(int idAgent) {
		this.idAgent = idAgent;
	}

	public Eae getEae() {
		return eae;
	}

	public void setEae(Eae eae) {
		this.eae = eae;
	}

}
