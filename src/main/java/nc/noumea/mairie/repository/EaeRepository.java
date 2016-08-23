package nc.noumea.mairie.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import nc.noumea.mairie.eae.domain.EaeEvalue;

import org.springframework.stereotype.Repository;

@Repository
public class EaeRepository {

	@PersistenceContext(unitName = "eaePersistenceUnit")
	private EntityManager eaeEntityManager;

	public void persistEntity(Object obj) {
		eaeEntityManager.persist(obj);
	}
	
	public <T> T findEntity(Class<T> entityClass, Integer primaryKey) {
		return eaeEntityManager.find(entityClass, primaryKey);
	}
	
	public EaeEvalue getEaeEvalue(Integer idEae) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("select eval from EaeEvalue eval ");
		sb.append("where eval.eae.idEae = :idEae ");
		
		TypedQuery<EaeEvalue> query = eaeEntityManager.createQuery(sb.toString(), EaeEvalue.class);
		
		query.setParameter("idEae", idEae);
		
		List<EaeEvalue> listEaeEvalue = query.getResultList();
		
		if(listEaeEvalue.isEmpty()) {
			return null;
		}
		
		return listEaeEvalue.get(0);
	}
}
