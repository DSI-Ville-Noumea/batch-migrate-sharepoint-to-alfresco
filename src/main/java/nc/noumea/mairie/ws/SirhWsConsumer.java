package nc.noumea.mairie.ws;

import java.util.HashMap;
import java.util.Map;

import nc.noumea.mairie.dto.AgentGeneriqueDto;

import org.springframework.stereotype.Service;

import com.sun.jersey.api.client.ClientResponse;

@Service
public class SirhWsConsumer extends BaseWsConsumer implements ISirhWsConsumer {

	private String sirhWsBaseUrl;
	
	private static final String sirhAgentUrl = "agents/getAgent";

	@Override
	public AgentGeneriqueDto getAgent(Integer idAgent) {
		
		String url = String.format(sirhWsBaseUrl + sirhAgentUrl);

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("idAgent", String.valueOf(idAgent));

		ClientResponse res = createAndFireGetRequest(parameters, url);

		return readResponse(AgentGeneriqueDto.class, res, url);
	}

	public String getSirhWsBaseUrl() {
		return sirhWsBaseUrl;
	}

	public void setSirhWsBaseUrl(String sirhWsBaseUrl) {
		this.sirhWsBaseUrl = sirhWsBaseUrl;
	}
	
	
}
