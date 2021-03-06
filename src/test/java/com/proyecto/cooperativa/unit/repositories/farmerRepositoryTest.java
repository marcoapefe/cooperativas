package com.proyecto.cooperativa.unit.repositories;

import com.proyecto.cooperativa.models.Constants;
import com.proyecto.cooperativa.models.Farmer;
import com.proyecto.cooperativa.models.Person;
import com.proyecto.cooperativa.repositories.FarmerRepository;
import com.proyecto.cooperativa.repositories.PersonRepository;
import lombok.NonNull;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.proyecto.cooperativa.models.Constants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.util.StringUtils.isEmpty;

@RunWith(SpringRunner.class)
@SpringBootTest
public class farmerRepositoryTest {
    private static final Logger log = LoggerFactory.getLogger(farmerRepositoryTest.class);

    @Autowired
    FarmerRepository farmerRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final String EMAIL = "email";
    private static final String PHONE = "telefono";
    private static final String CIF_NIF = "cif_nif";
    private static final String PERCENT = "%";

    @Test
    public void testGetFarmerList() throws Exception {
        final String select = "SELECT  a.n_socio, " +
                "p.cif_nif, " +
                "p.nombre_razon_social, " +
                "p.apellidos, " +
                "p.direccion, " +
                "p.telefono, " +
                "p.email " +
                "FROM PERSONAS p " +
                "JOIN AGRICULTORES a ON (p.id_persona = a.id_persona) ";
        testWithTextToSearch(select);
        testNoSearchingText(select);
    }

    private void testNoSearchingText(String sqlExpected) throws Exception {
        //given
        String textToSearch = "";
        //when
        List<Map<String, Object>> actual = farmerRepository.getFarmersList(textToSearch);
        List<Map<String, Object>> expected = jdbcTemplate.queryForList(sqlExpected);
        //then
        assertActualAndExpectedLists(actual, expected);
    }

    private void testWithTextToSearch(String select) throws Exception {
        //given
        String textToSearch = "usuario";
        final String sqlExpected = select
                + " WHERE a.n_socio like ? "
                + "OR p.cif_nif like ? "
                + "OR p.nombre_razon_social like ? "
                + "OR p.apellidos like ? "
                + "OR p.direccion like ? "
                + "OR p.telefono like ?  "
                + "OR p.email like ? ";
        //when
        List<Map<String, Object>> actual = farmerRepository.getFarmersList(textToSearch);
        List<Map<String, Object>> expected = getFilteredList(sqlExpected, textToSearch);
        //then
        assertActualAndExpectedLists(actual, expected);
    }

    private List<Map<String, Object>> getFilteredList(String sqlExpected, String textToSearch) {
        textToSearch = PERCENT + textToSearch + PERCENT;
        return jdbcTemplate.queryForList(sqlExpected,
                textToSearch,
                textToSearch,
                textToSearch,
                textToSearch,
                textToSearch,
                textToSearch,
                textToSearch);
    }

    private void assertActualAndExpectedLists(List<Map<String, Object>> actual,
                                              List<Map<String, Object>> expected) {
        assertThat(actual, is(expected));
        assertThat(actual.size(), is(expected.size()));
        assertEquals(actual, expected);
        assertThat(actual, not(IsEmptyCollection.empty()));
        assertValidationStringFromField(EMAIL, actual);
        assertValidationStringFromField(PHONE, actual);
        assertValidationStringFromField(CIF_NIF, actual);
    }

    private void assertValidationStringFromField(String field, List<Map<String, Object>> actual) {
        long ZERO = 0;
        long incorrectMaps = actual.stream()
                .filter(map -> map.containsKey(field)
                        && !isEmpty(map.get(field))
                        && !map.get(field).toString().matches(getValidationStringFrom(field)))
                .count();
        assertThat(incorrectMaps, is(ZERO));
    }

    private String getValidationStringFrom(@NonNull String field) {
        switch (field) {
            case EMAIL:
                return EMAIL_VALIDATION;
            case PHONE:
                return PHONE_NUMBER_VALIDATION;
            case CIF_NIF:
                return CIF_NIF_VALIDATION;
            default:
                return "";
        }
    }


    @Test
    public void testCreateFarmer() {
        //Given
        int personId = 7;
        Farmer current = createDummyFarmer(personId);
        assertTrue(farmerRepository.create(current));
        //when
        Farmer expected = readFarmer(personId);
        boolean isDeleted = deleteFarmer(personId);
        //then
        assertCreations(current, expected, isDeleted);
    }

    private Farmer createDummyFarmer(@NonNull int personId) {
        Farmer farmer = new Farmer();
        farmer.setPersonId(personId);
        farmer.setDropOut(false);
        return farmer;
    }

    private Farmer readFarmer(int personId) {
        final String readSql = "SELECT id_persona, baja FROM AGRICULTORES WHERE ID_PERSONA = ?";
        return jdbcTemplate.queryForObject(readSql, new Integer[]{personId}, new FarmerRowMapper());
    }

    private boolean deleteFarmer(int personId) {
        final String deleteSql = "DELETE FROM AGRICULTORES WHERE ID_PERSONA = ?";
        return jdbcTemplate.update(deleteSql, personId) > 0;
    }

    private void assertCreations(@NonNull Farmer current,
                                 @NonNull Farmer expected,
                                 @NonNull boolean isDeleted) {
        assertEquals(current, expected);
        assertTrue(isDeleted);
    }

    class FarmerRowMapper implements RowMapper<Farmer> {
        @Override
        public Farmer mapRow(ResultSet rs, int rowNumber) throws SQLException {
            Farmer farmer = new Farmer();
            farmer.setPersonId(rs.getInt("id_persona"));
            farmer.setDropOut(rs.getBoolean("baja"));
            return farmer;
        }

    }

    @Test
    public void dropOutFarmer() {
        int personId = 6;
        Farmer current = createDummyFarmer(personId);
        assertTrue(farmerRepository.create(current));
        current.setDropOut(true);
        farmerRepository.dropOut(current);
        Farmer expected = readFarmer(current.getPersonId());
        assertEquals(current, expected);
        assertTrue(deleteFarmer(current.getPersonId()));
    }

    @Test
    public void readFarmer() {
        int personId = 20;
        Farmer actual = createDummyFarmer(personId);
        Person person = createDummyPerson();
        boolean isPersonCreated = personRepository.create(person);
        boolean isFarmerCreated = farmerRepository.create(actual);
        Farmer expected = farmerRepository.read(personId);
        assertActualAndExceptedFarmer(actual, expected);

    }


    private Person createDummyPerson(){
        Person person = new Person();
        person.setPhoneNumber("956123456");
        person.setAdress("C/hola");
        person.setName("Juan");
        person.setLastName("Troya");
        person.setCifNif("54129784s");
        person.setEmail("meritorio@meritocracia.es");
        return person;
    }

    private void assertActualAndExceptedFarmer(Farmer actual, Farmer expected) {
        assertThat(actual, not(isEmpty(actual)));
        assertThat(expected, not(isEmpty(expected)));
        assertThat(actual, is(expected));
        assertEquals(actual, expected);
        validateStringFrom(EMAIL, expected);
        validateStringFrom(PHONE, expected);
        validateStringFrom(CIF_NIF, expected);
        assertTrue(deleteFarmer(actual.getPersonId()));

    }

    private void validateStringFrom(String field, Farmer farmer) {
        boolean isValidated = false;
        switch (field) {
            case EMAIL:
                isValidated = farmer.getEmail().matches(Constants.EMAIL_VALIDATION);
                break;
            case PHONE:
                isValidated = farmer.getPhoneNumber().matches(Constants.PHONE_NUMBER_VALIDATION);
                break;
            case CIF_NIF:
                isValidated = farmer.getCifNif().matches(Constants.CIF_NIF_VALIDATION);
                break;
        }
        assertTrue(isValidated);
    }

}

