package org.runningdinner.core.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.runningdinner.core.CoreUtil;
import org.runningdinner.core.FuzzyBoolean;
import org.runningdinner.core.Gender;
import org.runningdinner.core.Participant;
import org.runningdinner.core.ParticipantAddress;
import org.runningdinner.core.ParticipantName;
import org.runningdinner.core.RunningDinnerConfig;
import org.runningdinner.core.converter.ConversionException.CONVERSION_ERROR;
import org.runningdinner.core.converter.ConverterFactory.INPUT_FILE_TYPE;
import org.runningdinner.core.converter.config.EmailColumnConfig;
import org.runningdinner.core.converter.config.MobileNumberColumnConfig;
import org.runningdinner.core.converter.config.ParsingConfiguration;

public class ConverterTest {

	public static final String STANDARD_XLS_FILE = "/excelimport/standard.xls";
	public static final String STANDARD_XLS_WITH_CONTACTINFO_FILE = "/excelimport/standard_with_contact.xls";
	public static final String STANDARD_XLSX_FILE = "/excelimport/standard.xlsx";
	public static final String EMPTY_XLS_FILE = "/excelimport/empty.xls";
	public static final String INVALID_XLSX_FILE = "/excelimport/invalid.xlsx";

	private InputStream inputStream;

	@Test
	public void testFileTypeRecognition() throws IOException, ConversionException {
		INPUT_FILE_TYPE fileType = ConverterFactory.determineFileType(STANDARD_XLS_FILE);
		assertEquals(INPUT_FILE_TYPE.HSSF, fileType);

		fileType = ConverterFactory.determineFileType("foo");
		assertEquals(INPUT_FILE_TYPE.UNKNOWN, fileType);

		fileType = ConverterFactory.determineFileType(".xlsx");
		assertEquals(INPUT_FILE_TYPE.XSSF, fileType);
	}

	@Test
	public void testConfigurations() {
		ParsingConfiguration parsingConfiguration = ParsingConfiguration.newDefaultConfiguration();

		assertEquals(false, parsingConfiguration.getSequenceColumnConfig().isAvailable());

		assertEquals(true, parsingConfiguration.getNameColumnConfig().isAvailable());
		assertEquals(true, parsingConfiguration.getNameColumnConfig().isComposite());
		assertEquals(0, parsingConfiguration.getNameColumnConfig().getFirstnameColumn());
		assertEquals(0, parsingConfiguration.getNameColumnConfig().getLastnameColumn());

		assertEquals(true, parsingConfiguration.getNumSeatsColumnConfig().isAvailable());
		assertEquals(true, parsingConfiguration.getNumSeatsColumnConfig().isNumericDeclaration());
		assertEquals(3, parsingConfiguration.getNumSeatsColumnConfig().getColumnIndex());

		assertEquals(true, parsingConfiguration.getAddressColumnConfig().isStreetAndStreetNrCompositeConfig());
		assertEquals(true, parsingConfiguration.getAddressColumnConfig().isZipAndCityCompositeConfig());
		assertEquals(false, parsingConfiguration.getAddressColumnConfig().isCompositeConfig());
		assertEquals(false, parsingConfiguration.getAddressColumnConfig().isSingleColumnConfig());
		assertEquals(1, parsingConfiguration.getAddressColumnConfig().getStreetColumn());
		assertEquals(1, parsingConfiguration.getAddressColumnConfig().getStreetNrColumn());
		assertEquals(2, parsingConfiguration.getAddressColumnConfig().getZipColumn());
		assertEquals(2, parsingConfiguration.getAddressColumnConfig().getCityColumn());
	}

	@Test
	public void testNameParsing() {
		ParticipantName participantName = ParticipantName.newName().withFirstname("Clemens").andLastname("Stich");
		assertEquals("Clemens", participantName.getFirstnamePart());
		assertEquals("Stich", participantName.getLastname());
		assertEquals("Clemens Stich", participantName.getFullnameFirstnameFirst());

		participantName = ParticipantName.newName().withCompleteNameString("Clemens Stich");
		assertEquals("Clemens", participantName.getFirstnamePart());
		assertEquals("Stich", participantName.getLastname());

		participantName = ParticipantName.newName().withCompleteNameString("Firstname Middlename Lastname");
		assertEquals("Firstname Middlename", participantName.getFirstnamePart());
		assertEquals("Lastname", participantName.getLastname());

		try {
			participantName = ParticipantName.newName().withCompleteNameString("Invalid");
			fail("Expected IllegalARgumentException to be thrown!");
		}
		catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testAddressParsing() {
		String addressStr = "Musterstrasse 1\r\n12345 Musterstadt";
		ParticipantAddress address = ParticipantAddress.parseFromString(addressStr);
		assertEquals("Musterstadt", address.getCityName());
		assertEquals(12345, address.getZip());
		assertEquals("Musterstrasse", address.getStreet());

		address = new ParticipantAddress();
		address.setStreetAndNr("Musterstraße 8a");
		assertEquals("Musterstraße", address.getStreet());
		assertEquals("8a", address.getStreetNr());
		address.setStreetAndNr("Im Acker 8a");
		assertEquals("Im Acker", address.getStreet());
		assertEquals("8a", address.getStreetNr());

		address.setZipAndCity("79100 TwoWord City");
		assertEquals(79100, address.getZip());
		assertEquals("TwoWord City", address.getCityName());

		try {
			address.setZipAndCity("12345612345 city");
			fail("Expected IllegalArgumentException to be thrown (Zip is too long)");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(true);
		}

		try {
			address.setStreetAndNr("onlystreet");
			fail("Expected IllegalArgumentException to be thrown");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(true);
		}
	}

	@Test
	public void testEmptyXls() throws IOException, ConversionException {
		inputStream = getClass().getResourceAsStream(EMPTY_XLS_FILE);
		FileConverter excelConverter = ConverterFactory.newConverter(ParsingConfiguration.newDefaultConfiguration(), INPUT_FILE_TYPE.HSSF);
		List<Participant> participants = excelConverter.parseParticipants(inputStream);
		assertEquals(0, participants.size());
	}

	@Test
	public void testInvalidXlsx() throws IOException {

		inputStream = getClass().getResourceAsStream(INVALID_XLSX_FILE);

		ParsingConfiguration parsingConfiguration = ParsingConfiguration.newDefaultConfiguration();
		parsingConfiguration.setStartRow(0);

		FileConverter excelConverter = ConverterFactory.newConverter(parsingConfiguration, INPUT_FILE_TYPE.XSSF);

		try {
			excelConverter.parseParticipants(inputStream);
			fail("Expected ConversionException to be thrown!");
		}
		catch (ConversionException e) {
			e.printStackTrace();
			assertEquals(CONVERSION_ERROR.NAME, e.getConversionError());
			assertEquals(2, e.getRowNumber());
		}
	}

	@Test
	public void testXls() throws IOException, ConversionException {

		INPUT_FILE_TYPE fileType = ConverterFactory.determineFileType(STANDARD_XLS_FILE);
		assertEquals(INPUT_FILE_TYPE.HSSF, fileType);

		ParsingConfiguration parsingConfiguration = ParsingConfiguration.newDefaultConfiguration();

		inputStream = getClass().getResourceAsStream(STANDARD_XLS_FILE);
		FileConverter excelConverter = ConverterFactory.newConverter(parsingConfiguration, fileType);
		List<Participant> participants = excelConverter.parseParticipants(inputStream);

		checkParsedParticipants(participants, false);
	}

	@Test
	public void testXlsx() throws IOException, ConversionException {

		INPUT_FILE_TYPE fileType = ConverterFactory.determineFileType(STANDARD_XLSX_FILE);
		assertEquals(INPUT_FILE_TYPE.XSSF, fileType);

		ParsingConfiguration parsingConfiguration = ParsingConfiguration.newDefaultConfiguration();

		inputStream = getClass().getResourceAsStream(STANDARD_XLSX_FILE);
		FileConverter excelConverter = ConverterFactory.newConverter(parsingConfiguration, fileType);
		List<Participant> participants = excelConverter.parseParticipants(inputStream);

		checkParsedParticipants(participants, false);
	}

	@Test
	public void testXlsWithContactInfo() throws IOException, ConversionException {

		INPUT_FILE_TYPE fileType = ConverterFactory.determineFileType(STANDARD_XLS_WITH_CONTACTINFO_FILE);
		assertEquals(INPUT_FILE_TYPE.HSSF, fileType);

		ParsingConfiguration parsingConfiguration = ParsingConfiguration.newDefaultConfiguration();
		parsingConfiguration.setEmailColumnConfig(EmailColumnConfig.createEmailColumnConfig(4));
		parsingConfiguration.setMobileNumberColumnConfig(MobileNumberColumnConfig.createMobileNumberColumnConfig(5));

		inputStream = getClass().getResourceAsStream(STANDARD_XLS_WITH_CONTACTINFO_FILE);
		FileConverter excelConverter = ConverterFactory.newConverter(parsingConfiguration, fileType);
		List<Participant> participants = excelConverter.parseParticipants(inputStream);

		checkParsedParticipants(participants, true);
	}

	@Test
	public void testIntegrationParsingAndCalculation() {
		INPUT_FILE_TYPE fileType = ConverterFactory.determineFileType("/excelimport/standard.xls");
	}

	private void checkParsedParticipants(List<Participant> participants, boolean checkContactInfo) {
		assertEquals(4, participants.size());

		Participant first = participants.get(0);
		assertEquals(1, first.getParticipantNumber());
		assertEquals(Gender.UNDEFINED, first.getGender());
		assertEquals(4, first.getNumSeats());
		assertEquals("Clemens Stich", first.getName().getFullnameFirstnameFirst());
		if (checkContactInfo) {
			assertEquals("c.s@mail.de", first.getEmail());
			assertEquals("0176 22", first.getMobileNumber());
		}

		Participant second = participants.get(1);
		assertEquals(2, second.getParticipantNumber());
		assertEquals(Gender.UNDEFINED, second.getGender());
		assertEquals(6, second.getNumSeats());
		assertEquals("Max Mustermann", second.getName().getFullnameFirstnameFirst());
		assertEquals("Musterstraße 12", second.getAddress().getStreetWithNr());
		assertEquals("11111 Musterhausen", second.getAddress().getZipWithCity());
		if (checkContactInfo) {
			assertEquals("mm@mm.de", second.getEmail());
			assertEquals(StringUtils.EMPTY, second.getMobileNumber());
		}

		Participant third = participants.get(2);
		assertEquals("Michaela Musterfrau", third.getName().getFullnameFirstnameFirst());
		assertEquals(3, third.getParticipantNumber());
		assertEquals(StringUtils.EMPTY, third.getEmail());
		if (checkContactInfo) {
			assertEquals("123456", third.getMobileNumber());
		}

		Participant fourth = participants.get(3);
		assertEquals("Biene Maja", fourth.getName().getFullnameFirstnameFirst());
		assertEquals(4, fourth.getParticipantNumber());
		assertEquals("Auf dem Bienenstock 1", fourth.getAddress().getStreetWithNr());
		assertEquals("12345 Bienenstadt", fourth.getAddress().getZipWithCity());
		assertEquals(1000, fourth.getNumSeats());
		if (checkContactInfo) {
			assertEquals("maja@biene.de", fourth.getEmail());
		}

		RunningDinnerConfig standardConfig = RunningDinnerConfig.newConfigurer().build();
		assertEquals(FuzzyBoolean.FALSE, standardConfig.canHost(first));
		assertEquals(FuzzyBoolean.TRUE, standardConfig.canHost(second));
		assertEquals(FuzzyBoolean.FALSE, standardConfig.canHost(third));
		assertEquals(FuzzyBoolean.TRUE, standardConfig.canHost(fourth));
	}

	@After
	public void tearDown() {
		CoreUtil.closeStream(inputStream);
	}
}
