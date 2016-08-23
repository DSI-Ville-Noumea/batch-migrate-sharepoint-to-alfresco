package nc.noumea.mairie.writer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NO_CONTENT, reason = "Pas de document sous Sharepoint")
public class NoContentException extends RuntimeException {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 3868517091786304830L;

}
