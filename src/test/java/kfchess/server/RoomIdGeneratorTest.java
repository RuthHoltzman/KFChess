package kfchess.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomIdGeneratorTest {

    @Test
    void generate_returnsSixCharacterCode() {
        assertEquals(6, RoomIdGenerator.generate().length());
    }

    @Test
    void generate_onlyUppercaseLettersAndDigits() {
        assertTrue(RoomIdGenerator.generate().matches("[A-Z0-9]{6}"));
    }

    @Test
    void generate_twoCallsProduceDifferentCodes() {
        // לא הבטחה מתמטית (יכול תיאורטית להתנגש) - אבל עם ~2 מיליארד
        // צירופים אפשריים, שני קריאות רצופות שמייצרות בדיוק אותו קוד
        // כמעט בלתי אפשרי בפועל; מספיק כ"בדיקת שפיות" שהמחלקה בכלל מייצרת
        // ערכים אקראיים ולא מחזירה קבוע.
        assertNotEquals(RoomIdGenerator.generate(), RoomIdGenerator.generate());
    }
}
