package browser;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Cookie;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Controller_login {
    private Scene loginScene;
    private Scene liveScene;
    private Controller_live liveController;

    @FXML private FlowPane loginForm;
    private HTTPClient client;
    private Stage stage;
    private JSONObject response = null;

    public void initialize () {
        client = new HTTPClient();
        populateForm();
    }

    public void populateForm() {
        loginForm.setVgap(10);
        ((GridPane) loginForm.getParent()).setGridLinesVisible(true);
        TextField login = new TextField();
        PasswordField password = new PasswordField();
        Label errorMsg = new Label("Login was unsuccessful, please try again.");
        login.setPromptText("Username");
        password.setPromptText("Password");
        errorMsg.setVisible(false);
        errorMsg.setId("failedLogin");
        password.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if(keyEvent.getCode().equals(KeyCode.ENTER)) {
                    try {
                        if(submitLogin(login.getText(), password.getText())) {
                            liveController.setClientAndLoginInfo(client, response);
                            stage.setScene(liveScene);
                        }
                        else {
                            password.clear();
                            errorMsg.setVisible(true);
                            login.requestFocus();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        setTestingLoginInfo(login, password);
        loginForm.getChildren().addAll(login, password, errorMsg);
    }

    private boolean submitLogin(String username, String password) throws IOException {
        //parseLoginForm(getLoginPage());
        //String page = getLoginPage();
        String url = "https://ssl.meetme.com/mobile/login";
        String params = "emailId="+ URLEncoder.encode(username, "UTF-8");
        params += "&password="+URLEncoder.encode(password, "UTF-8");
        params += "&fbAccessToken=";
        params += "&lat=";
        params += "&long=";
        params += "&systemInfo=";
        params += "&rememberMe=true";
        ArrayList<PropertyEntry> properties = new ArrayList<PropertyEntry>();
        properties.add(new PropertyEntry("Accept","application/json, text/javascript, */*; q=0.01"));
        properties.add(new PropertyEntry("Connection","keep-alive;"));
        properties.add(new PropertyEntry("Content-Type","application/x-www-form-urlencoded; charset=UTF-8"));
        // This next line is the key for avoiding a 302 response
        properties.add(new PropertyEntry("x-device","phoenix/screen_medium,51d344ef-21a3-4944-9a2d-c2f20d81ad3a,5.20.4"));
        //client.sendOptionsRequest(url, properties);
        String loginResponse = client.sendRequest("POST", url, params, properties);
        List<Cookie> loginCookies = new ArrayList<Cookie>();
        for(String cookie : client.getCookies()) {
            String[] parsedCookie = cookie.split(";");
            String path = "";
            SimpleDateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z");
            Date expiry = null;
            String domain = "";
            boolean isSecure = false;
            boolean isHttpOnly = false;
            for(String dataEntry : parsedCookie) {
                if(dataEntry.startsWith(" domain="))
                    domain = dataEntry.substring(" domain=".length());
                else if(dataEntry.startsWith(" path="))
                    path = dataEntry.substring(" path=".length());
                else if(dataEntry.startsWith(" expires=")) {
                    try{
                        expiry = df.parse(dataEntry.substring(" expires=".length()));
                    } catch(ParseException e) {e.printStackTrace();}
                }
                else if(dataEntry.startsWith(" httponly"))
                    isHttpOnly = true;
            }
            parsedCookie = parsedCookie[0].split("=");
            Cookie formattedCookie = new Cookie(parsedCookie[0], parsedCookie[1], domain, path, expiry, isSecure, isHttpOnly);
            loginCookies.add(formattedCookie);
            System.out.println("Login Cookies Added: " + formattedCookie);
        }
        client.setLoginCookies(loginCookies);
        try {
            response = (JSONObject) new JSONParser().parse(loginResponse);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if(response.get("error") != null) {
            if(response.get("errorType").equals("LoginException")) {
                //TODO: special handling?
            }
//            System.out.println("\n"+loginResponse);
//            System.out.println(response.toString());
//            System.out.println(response.toJSONString());
            return false;
        }
        else {
            return true;
        }
    }

    public void setStageAndSetupListeners(Stage stage, Scene loginScene, Scene liveScene, Controller_live controller_live){
        this.stage = stage;
        this.loginScene = loginScene;
        this.liveScene = liveScene;
        this.liveController = controller_live;
    }

    private String getLoginPage() throws IOException {
        CookieHandler.setDefault(new CookieManager());
        String loginUrl = "https://ssl.meetme.com/mobile/login/";
        ArrayList<PropertyEntry> properties = new ArrayList<PropertyEntry>();
        properties.add(new PropertyEntry("Accept","application/json, text/javascript, */*; q=0.01"));
        properties.add(new PropertyEntry("Accept-Language","en-US,en;q=0.9"));
        return client.sendRequest("GET", loginUrl, null, properties);
    }

    private void parseLoginForm(String page, String un, String pass) throws UnsupportedEncodingException {
        Document document = Jsoup.parse(page);
        Element form = document.getElementById("loginForm");
        Elements inputs = form.getElementsByTag("input");

        List<String> paramList = new ArrayList<String>();

    }

    private void setTestingLoginInfo(TextField username, TextField password) {
        username.setText("mcluvinisnotben@gmail.com");
        password.setText("1234pass");
    }
}
