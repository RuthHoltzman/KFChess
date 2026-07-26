package texttests;

import kfchess.server.client.GameClient;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * GameClient מזויף לבדיקות: דורס את שיטות השליחה כדי רק לרשום מה נקרא,
 * בלי לגעת ברשת בכלל - GameClient.send() האמיתית (שקוראים לה sendClick/
 * sendJump/sendRestart) הייתה זורקת NotYetConnectedException כי הטסטים
 * כאן לא באמת מתחברים לשרת. אותה גישה בדיוק כמו FakeWebSocket שכבר קיים
 * (מחלקת stub ייעודית לטסטים, בחבילה הזו).
 */
class RecordingGameClient extends GameClient {

    boolean restartSent = false;
    boolean clickSent = false;
    boolean jumpSent = false;

    RecordingGameClient() throws URISyntaxException {
        super(new URI("ws://localhost:1"));
    }

    @Override
    public void sendRestart() {
        restartSent = true;
    }

    @Override
    public void sendClick(int row, int col) {
        clickSent = true;
    }

    @Override
    public void sendJump(int row, int col) {
        jumpSent = true;
    }
}
