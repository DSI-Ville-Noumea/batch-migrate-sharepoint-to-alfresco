package nc.noumea.mairie.ws;

import nc.noumea.mairie.dto.AgentGeneriqueDto;

public interface ISirhWsConsumer {

	AgentGeneriqueDto getAgent(Integer idAgent);	
}
