package nc.noumea.mairie.eae.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

@Entity
@Table(name = "EAE")
@PersistenceUnit(unitName = "eaePersistenceUnit")
public class Eae {

	@Id
	@Column(name = "ID_EAE")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idEae;

	@OneToOne(mappedBy = "eae", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private EaeEvalue eaeEvalue;

	@OneToMany(mappedBy = "eae", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<EaeFinalisation> eaeFinalisations = new HashSet<EaeFinalisation>();

	public Integer getIdEae() {
		return idEae;
	}

	public void setIdEae(Integer idEae) {
		this.idEae = idEae;
	}

	public EaeEvalue getEaeEvalue() {
		return eaeEvalue;
	}

	public void setEaeEvalue(EaeEvalue eaeEvalue) {
		this.eaeEvalue = eaeEvalue;
	}

	public Set<EaeFinalisation> getEaeFinalisations() {
		return eaeFinalisations;
	}

	public void setEaeFinalisations(Set<EaeFinalisation> eaeFinalisations) {
		this.eaeFinalisations = eaeFinalisations;
	}

	
}
