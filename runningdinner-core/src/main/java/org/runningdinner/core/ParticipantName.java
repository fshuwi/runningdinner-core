package org.runningdinner.core;

import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the name of a participant.
 * 
 * @author Clemens Stich
 * 
 */
@Embeddable
public class ParticipantName {

	private String firstnamePart;
	private String lastname;

	public ParticipantName() {
		// Unfortunately needed for JPA & Spring MVC
	}

	/**
	 * Contains typically the firstname of a person, but for persons that have several names (e.g. middlename) these name parts are also
	 * contained.
	 * 
	 * @return
	 */
	public String getFirstnamePart() {
		return firstnamePart;
	}

	/**
	 * Contains always the surname of a person
	 * 
	 * @return
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * Returns the fullname of a participant
	 * 
	 * @return
	 */
	public String getFullnameFirstnameFirst() {
		String result = firstnamePart;
		if (StringUtils.isEmpty(firstnamePart)) {
			result = StringUtils.EMPTY;
		}
		else {
			if (!StringUtils.isEmpty(lastname)) {
				result += " ";
			}
		}

		if (!StringUtils.isEmpty(lastname)) {
			result += lastname;
		}

		return result;
	}

	// Unfortunately needed by Spring MVC

	public void setFirstnamePart(String firstnamePart) {
		this.firstnamePart = firstnamePart;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	@Override
	public String toString() {
		return getFullnameFirstnameFirst();
	}

	/**
	 * Use this for creating new ParticipantName instances
	 * 
	 * @return
	 */
	public static NameBuilder newName() {
		return new NameBuilder();
	}

	/**
	 * Builder for creating new ParticipantNames in a fluent way
	 * 
	 * @author Clemens Stich
	 * 
	 */
	public static class NameBuilder {

		protected NameBuilder() {
		}

		/**
		 * Construct a ParticipantName with passing in firstname and lastname separately.
		 * 
		 * @param firstname
		 * @return
		 */
		public FirstLastNameBuilder withFirstname(final String firstname) {
			CoreUtil.assertNotEmpty(firstname, "Firstname must not be empty!");
			return new FirstLastNameBuilder(firstname);
		}

		/**
		 * Construct a ParticipantName by using a complete string in the following formats:<br>
		 * Peter Lustig<br>
		 * Max Middlename Mustermann<br>
		 * 
		 * @param firstname
		 * @throws IllegalArgumentException If string was passed in wrong format
		 * @return
		 */
		public ParticipantName withCompleteNameString(final String completeName) {
			ParticipantName result = new ParticipantName();

			String[] nameParts = completeName.trim().split("\\s+");
			if (nameParts.length <= 1) {
				throw new IllegalArgumentException("Complete Name must be in a format like 'Max Mustermann'");
			}

			result.lastname = nameParts[nameParts.length - 1];

			StringBuilder firstnamesBuilder = new StringBuilder();
			int cnt = 0;
			for (int i = 0; i < nameParts.length - 1; i++) {
				if (cnt++ > 0) {
					firstnamesBuilder.append(" ");
				}
				firstnamesBuilder.append(nameParts[i]);
			}
			result.firstnamePart = firstnamesBuilder.toString();

			return result;
		}

	}

	public static class FirstLastNameBuilder {

		private String firstname;

		protected FirstLastNameBuilder(String firstname) {
			this.firstname = firstname;
		}

		public ParticipantName andLastname(String lastname) {
			CoreUtil.assertNotEmpty(lastname, "Lastname must not be empty!");
			ParticipantName result = new ParticipantName();
			result.firstnamePart = firstname;
			result.lastname = lastname;
			return result;
		}
	}
}
