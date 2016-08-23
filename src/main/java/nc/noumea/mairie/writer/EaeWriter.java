package nc.noumea.mairie.writer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.noumea.mairie.alfresco.cmis.CmisService;
import nc.noumea.mairie.alfresco.cmis.CmisUtils;
import nc.noumea.mairie.alfresco.cmis.CreateSession;
import nc.noumea.mairie.dto.AgentGeneriqueDto;
import nc.noumea.mairie.eae.domain.EaeEvalue;
import nc.noumea.mairie.eae.domain.EaeFinalisation;
import nc.noumea.mairie.repository.EaeRepository;
import nc.noumea.mairie.ws.ISirhWsConsumer;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class EaeWriter<T> implements ItemWriter<EaeFinalisation> {

	private static final Logger logger = LoggerFactory.getLogger(EaeWriter.class);

	protected static final String ERROR_PATH = "Erreur lors de l'envoi du fichier : le dossier distant n'existe pas : ";
	protected static final String FILE_NOT_FOUND = "Fichier non trouvé : ";
	protected static final String AGENT_NOT_FOUND = "Agent non trouvé : ";

	@Autowired
	private ISirhWsConsumer sirhWsConsumer;
	
	@Autowired
	private EaeRepository eaeRepository;
	
	@Autowired
	private CmisService cmisService;
	
	@Autowired
	private CreateSession createSession;
	
	private SessionFactory sessionFactory;
	
	private String alfrescoUrl;
	private String alfrescoLogin;
	private String alfrescoPassword;
	
	private String kiosqueUserWebdav;
	private String kiosqueUserPwsWebdav;
	private String kiosqueDomainWebdav;
	private String kiosqueUrlWebdav;
	private String kiosquePortWebdav;
	private String kiosqueUrlGedSharepoint;

	@Override
	@Transactional(value = "eaeTransactionManager")
	public void write(List<? extends EaeFinalisation> items) throws Exception {
		
		logger.info("START EaeWriter write with items size : " + items.size()); 
		
		Session session = createSession.getSession(alfrescoUrl, alfrescoLogin, alfrescoPassword);
		
		for (EaeFinalisation eaeFinal : items) {
			
			// on cherche le repertoire distant 
			CmisObject object = null;
			
			eaeFinal = eaeRepository.findEntity(EaeFinalisation.class, eaeFinal.getIdEaeFinalisation());
			
			EaeEvalue evalue = eaeRepository.getEaeEvalue(eaeFinal.getEae().getIdEae());
			
			AgentGeneriqueDto agentDto = sirhWsConsumer.getAgent(evalue.getIdAgent());
			if(null == agentDto) {
				logger.error(AGENT_NOT_FOUND + evalue.getIdAgent());
				eaeFinal.setCommentaireAlfresco(AGENT_NOT_FOUND + evalue.getIdAgent());
				continue;
			}
				
		    String nom = agentDto.getDisplayNom();
	    	String prenom = agentDto.getDisplayPrenom();
	    	
	    	// repertoire distant alfresco
			String pathDestination = getPathOfDestinationFolder(evalue.getIdAgent(), prenom, nom);
			
			try {
				object = session.getObject(cmisService.getIdObjectCmis(pathDestination, session));
			} catch(CmisObjectNotFoundException e) {
				// si non trouve, on cree l arborescence agent
				logger.debug(ERROR_PATH + pathDestination);
				cmisService.createArborescenceAgent(evalue.getIdAgent(), nom, prenom, session);
			}

			// une fois l arborescence creee, on recherche de nouveau le dossier
			// distant alfresco
			try {
				object = session.getObject(cmisService.getIdObjectCmis(pathDestination, session));
			} catch (CmisObjectNotFoundException e) {
				logger.error(ERROR_PATH + pathDestination);
				eaeFinal.setCommentaireAlfresco(ERROR_PATH + pathDestination);
				continue;
			}
			
		    if(null == object) {
		    	logger.error(ERROR_PATH + pathDestination);
				eaeFinal.setCommentaireAlfresco(ERROR_PATH + pathDestination);
		    	continue;
		    }

		    Folder folder = (Folder) object;
		    InputStream stream = null;
		    Document doc = null;
		    String nomEae = null;
		    
		    try {
			    nomEae = getNomEAE(eaeFinal.getIdGedDocument());
			    
			    Map<String, Object> properties = new HashMap<String, Object>();
				properties.put(PropertyIds.NAME, nomEae);
				properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
				properties.put(PropertyIds.DESCRIPTION, eaeFinal.getCommentaire());
				
				stream =  copyEaeToLocalPath(nomEae, 0);
				byte[] bytes = IOUtils.toByteArray(stream);
				
				ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
			    ContentStream contentStream = new ContentStreamImpl(
			    	nomEae, 
		    		BigInteger.valueOf(bytes.length), 
		    		"application/pdf", 
		    		byteStream);
		    
				// create a major version
		    	doc = folder.createDocument(properties, contentStream, VersioningState.MAJOR);
		    } catch(CmisContentAlreadyExistsException e) {
		    	logger.error("CmisContentAlreadyExistsException "  + pathDestination + nomEae);
		    	logger.debug(e.getMessage());
		    	doc = (Document) session.getObject(cmisService.getIdObjectCmis(pathDestination + nomEae, session));
				eaeFinal.setCommentaireAlfresco("CmisContentAlreadyExistsException "  + pathDestination + nomEae);
		    } catch(CmisConstraintException e) {
		    	logger.error("CmisConstraintException " + pathDestination + nomEae);
		    	logger.debug(e.getMessage());
				eaeFinal.setCommentaireAlfresco("CmisConstraintException " + pathDestination + nomEae);
		    	continue;
		    } catch(NoContentException e) {
		    	logger.error("NoContentException " + pathDestination + nomEae);
				eaeFinal.setCommentaireAlfresco("NoContentException " + pathDestination + nomEae + " : " + e.getMessage());
		    	continue;
		    } catch(Exception e) {
		    	logger.error("Exception " + pathDestination + nomEae);
		    	logger.debug(e.getMessage());
				eaeFinal.setCommentaireAlfresco("Exception " + pathDestination + nomEae + " : " + e.toString());
		    	continue;
		    } finally {
				IOUtils.closeQuietly(stream);
			}
		    
			if(null != doc.getProperty("cmis:secondaryObjectTypeIds")) {
				List<Object> aspects = doc.getProperty("cmis:secondaryObjectTypeIds").getValues();
				if (!aspects.contains("P:mairie:customDocumentAspect")) {
					aspects.add("P:mairie:customDocumentAspect");
					HashMap<String, Object> props = new HashMap<String, Object>();
					props.put("cmis:secondaryObjectTypeIds", aspects);
					doc.updateProperties(props);
					logger.debug("Added aspect");
				} else {
					logger.debug("Doc already had aspect");
				}
			}
			
			HashMap<String, Object> props = new HashMap<String, Object>();
			props.put("mairie:idAgentOwner", evalue.getIdAgent());
			props.put("mairie:idAgentCreateur", eaeFinal.getIdAgent());
			props.put("mairie:commentaire", eaeFinal.getCommentaire());
			doc.updateProperties(props);
			
			// on sauvegarde l EAE avec l ID NODE ALFRESCO
			eaeFinal.setNodeRefAlfresco(doc.getProperty("alfcmis:nodeRef").getFirstValue().toString());
		}
		
		logger.info("FINISH EaeWriter write with items size : " + items.size()); 
	}

	private String getPathOfDestinationFolder(Integer idAgent, String prenom, String nom) {

		String folderAgent = CmisUtils.getPathAgent(idAgent, nom, prenom);
		
		return "/Sites/SIRH/documentLibrary/Agents/" + folderAgent + "/Carrière/EAE/";
	}
	
	public String downloadDocumentAccesNTLMAs(String url) throws Exception {

		HttpResponse response = createAndFireRequestNTLM(url);
		return readResponseNTLM(response);
	}

	public HttpResponse createAndFireRequestNTLM(String url) throws ClientProtocolException, IOException {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getAuthSchemes().register("ntlm", new NTLMSchemeFactory());

		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(kiosqueUrlWebdav, Integer.valueOf(kiosquePortWebdav)),
				new NTCredentials(kiosqueUserWebdav, kiosqueUserPwsWebdav, null, kiosqueDomainWebdav));

		HttpHost target = new HttpHost(kiosqueUrlWebdav, Integer.valueOf(kiosquePortWebdav), "http");

		// Make sure the same context is used to execute logically related
		// requests
		HttpContext localContext = new BasicHttpContext();

		// Execute a cheap method first. This will trigger NTLM authentication

		HttpGet httpget = new HttpGet(url);

		HttpResponse response1 = httpclient.execute(target, httpget, localContext);

		return response1;
	}

	public String readResponseNTLM(HttpResponse response) {

		if (response.getStatusLine().getStatusCode() == HttpStatus.NO_CONTENT.value()) {
			throw new NoContentException();
		}

		if (response.getStatusLine().getStatusCode() != HttpStatus.OK.value()) {
			return null;
		}

		String output = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			output = br.readLine();
		} catch (IOException e) {

		}

		return output == "" ? null : output;
	}
	
	public InputStream copyEaeToLocalPath(String nomEAE, int sequenceNumber) throws Exception {

		String urlDocument = "http://" + kiosqueUrlWebdav + "/kiosque-rh/EAE/" + nomEAE;

		HttpResponse response = createAndFireRequestNTLM(urlDocument);

		InputStream in = null;

		try {
			in = response.getEntity().getContent();
		} catch (Exception ex) {
			throw new Exception("An error occured while copying a remote EAE into the local file path", ex);
		} finally {
//			IOUtils.closeQuietly(in);
		}

		return in;
	}
	
	public String getNomEAE(String eaeId) throws Exception {
		
		String eaeRemoteFileUri = null;

		// GET url from sharepoint
		String url = kiosqueUrlGedSharepoint.concat(eaeId);
		String webDavUri = downloadDocumentAccesNTLMAs(url);
		logger.debug("Sharepoint WS query: URL [{}] Response [{}]", url, webDavUri);

		// format result
		eaeRemoteFileUri = String.format(webDavUri, kiosqueUserWebdav, kiosqueUserPwsWebdav);
		
		String nomEAE = eaeRemoteFileUri.substring(eaeRemoteFileUri.indexOf("EAE_"), eaeRemoteFileUri.length());
		
		return nomEAE;
	}

	public String getAlfrescoUrl() {
		return alfrescoUrl;
	}

	public void setAlfrescoUrl(String alfrescoUrl) {
		this.alfrescoUrl = alfrescoUrl;
	}

	public String getAlfrescoLogin() {
		return alfrescoLogin;
	}

	public void setAlfrescoLogin(String alfrescoLogin) {
		this.alfrescoLogin = alfrescoLogin;
	}

	public String getAlfrescoPassword() {
		return alfrescoPassword;
	}

	public void setAlfrescoPassword(String alfrescoPassword) {
		this.alfrescoPassword = alfrescoPassword;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public String getKiosqueUserWebdav() {
		return kiosqueUserWebdav;
	}

	public void setKiosqueUserWebdav(String kiosqueUserWebdav) {
		this.kiosqueUserWebdav = kiosqueUserWebdav;
	}

	public String getKiosqueUserPwsWebdav() {
		return kiosqueUserPwsWebdav;
	}

	public void setKiosqueUserPwsWebdav(String kiosqueUserPwsWebdav) {
		this.kiosqueUserPwsWebdav = kiosqueUserPwsWebdav;
	}

	public String getKiosqueDomainWebdav() {
		return kiosqueDomainWebdav;
	}

	public void setKiosqueDomainWebdav(String kiosqueDomainWebdav) {
		this.kiosqueDomainWebdav = kiosqueDomainWebdav;
	}

	public String getKiosqueUrlWebdav() {
		return kiosqueUrlWebdav;
	}

	public void setKiosqueUrlWebdav(String kiosqueUrlWebdav) {
		this.kiosqueUrlWebdav = kiosqueUrlWebdav;
	}

	public String getKiosquePortWebdav() {
		return kiosquePortWebdav;
	}

	public void setKiosquePortWebdav(String kiosquePortWebdav) {
		this.kiosquePortWebdav = kiosquePortWebdav;
	}

	public String getKiosqueUrlGedSharepoint() {
		return kiosqueUrlGedSharepoint;
	}

	public void setKiosqueUrlGedSharepoint(String kiosqueUrlGedSharepoint) {
		this.kiosqueUrlGedSharepoint = kiosqueUrlGedSharepoint;
	}
	
}
