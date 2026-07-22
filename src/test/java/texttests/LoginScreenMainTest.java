package texttests;

import kfchess.LoginScreenMain;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginScreenMainTest {

    @Test
    void validate_emptyUsername_reportsError() {
        assertEquals(Optional.of("Username is required"), LoginScreenMain.validate("", "s3cret"));
    }

    @Test
    void validate_blankUsername_reportsError() {
        assertEquals(Optional.of("Username is required"), LoginScreenMain.validate("   ", "s3cret"));
    }

    @Test
    void validate_nullUsername_reportsError() {
        assertEquals(Optional.of("Username is required"), LoginScreenMain.validate(null, "s3cret"));
    }

    @Test
    void validate_emptyPassword_reportsError() {
        assertEquals(Optional.of("Password is required"), LoginScreenMain.validate("ruth", ""));
    }

    @Test
    void validate_nullPassword_reportsError() {
        assertEquals(Optional.of("Password is required"), LoginScreenMain.validate("ruth", null));
    }

    @Test
    void validate_bothFieldsFilled_reportsNoError() {
        assertTrue(LoginScreenMain.validate("ruth", "s3cret").isEmpty());
    }
}
