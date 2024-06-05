package com.axonivy.connector.vertexai.validators;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@FacesValidator(value = "commonValidator")
public class CommonValidator implements Validator {
	private static final String NON_BREAKING_WHITE_SPACE_AND_ZERO_WIDTH_SPACE_PATTERN = "(?<!&amp;)(&nbsp;)|\\u200B";

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		if (isRequiredTextInputValidator(value)) {
			addGrowlErrorMessage("Please add some text content.");
		}
	}

	private static String sanitize(String text) {
		String sanitizedText = Jsoup.clean(removeWhiteSpace(text),
				Safelist.relaxed().removeTags("div", "p", "img", "br"));
		return StringUtils.trim(sanitizedText);
	}

	private static String removeWhiteSpace(String text) {
		return text.replaceAll(NON_BREAKING_WHITE_SPACE_AND_ZERO_WIDTH_SPACE_PATTERN, StringUtils.EMPTY);
	}

	private boolean isRequiredTextInputValidator(Object value) {
		return value == null || StringUtils.isBlank(value.toString())
				|| StringUtils.isBlank(sanitize(value.toString()));
	}

	private void addGrowlErrorMessage(String message) {
		throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
	}
}
