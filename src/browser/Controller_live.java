package browser;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class Controller_live {

    private Stage stage;
    private Scene loginScene;
    private Scene liveScene;
    private Controller_login loginController;

    private HTTPClient client;
    private JSONObject loginResponse;
    private Member user;

    @FXML private VBox profileInfo;
    @FXML private FlowPane menu;
    @FXML private FlowPane livePane;

    private String ACCESS_TOKEN;
    private String SESSION_TOKEN;
    private String INSTALLATION_ID = "80e12240-127e-ce0d-ddb5-56bd6a3f94bd";
    private String SESSION_ID;
    private String broadcastID;
    private ArrayList<ChatMessage> chatList;
    private HashMap<String, Member> totalViewers;
    private boolean shouldPollData = false;
    private boolean shouldScroll = true;
    private boolean firstScroll = true;
    private JSONObject broadcastMetadata;

    private void initialize() {

    }

    public void populateScene() {
        // Setup user
        user = new Member();
        user.setAsUser();
        user.parseMemInfo((JSONObject) loginResponse.get("member"));

        // Retrieve and set tokens
        JSONObject oauthData = getOauthInfo();
        ACCESS_TOKEN = oauthData.get("access_token").toString();
        SESSION_ID = ((JSONObject) oauthData.get("details")).get("session_id").toString();
        JSONObject userData = getUserInfo();
        SESSION_TOKEN = userData.get("sessionToken").toString();

        // Populate scene sections
        populateProfile();
        populateControls();
        populateFavs();
    }

    public void getLiveChat(String broadcastID) {
        String proxy = "localhost:8000";
        ChromeOptions options = new ChromeOptions().addArguments("--proxy-server=http://" + proxy);
        options.addArguments("ignore-certificate-errors");
        System.setProperty("webdriver.chrome.driver", "C:/Users/Benjamin/Documents/GitHub/External/chromedriver.exe");

        WebDriver driver = new ChromeDriver(options);
        EventFiringWebDriver eventHandler = new EventFiringWebDriver(driver);
        WebDriverEventCapture eventCapture = new WebDriverEventCapture();
        eventHandler.register(eventCapture);

        driver.navigate().to("https://beta.meetme.com");
        for (Cookie cookie : client.getLoginCookes()) {
            System.out.println("Cookie added: " + cookie);
            driver.manage().addCookie(cookie);
        }
        driver.navigate().to("https://beta.meetme.com/#live/view/" + broadcastID);


        WebDriverWait wait = new WebDriverWait(driver, 60);
        WebElement iFrame = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("iframe#web-live-iframe")));
        driver.switchTo().frame("web-live-iframe");
        WebElement liveChat = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#ChatHistoryContainer_" + broadcastID)));

        //String shit = (eventHandler.findElement(new By.ById("tmg-live-video-chat-box"))).toString();

        pollChatData(liveChat, wait);
    }

    private void pollChatData(WebElement liveChat, WebDriverWait wait) {
        shouldPollData = true;
        Thread thread = new Thread() {
            int chatSize = 0;

            public void run() {
                while (shouldPollData) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            List<WebElement> chatMsgs = null;
                            WebElement chat = liveChat;
                            try {
                                chatMsgs = chat.findElements(By.cssSelector("div.chat-cell"));
                            } catch (Exception e) {
                                // Happens when a battle starts
                                chat = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#ChatHistoryContainer_" + broadcastID)));
                                chatMsgs = chat.findElements(By.cssSelector("div.chat-cell"));
                            }
                            if (chatSize != chatMsgs.size()) {
                                for (int i = chatSize; i < chatMsgs.size(); i++) {
                                    String classProperties = chatMsgs.get(i).getAttribute("class");
                                    Member chatMember;
                                    // If welcome message
                                    if (classProperties.contains("chat-welcome-cell")) {
                                        //System.out.println("Welcome message");
                                    }
                                    // If modBot message
                                    else if (classProperties.contains("chat-modbot-cell")) {
                                        //System.out.println("ModBot message");
                                    }
                                    // If join message
                                    else if (classProperties.contains("join-cell")) {
                                        //System.out.println("Member joined stream");
                                    }
                                    // If kick message
                                    else if (classProperties.contains("tmg-live-video-chat-alert-background")) {
                                        //System.out.println("Member was kicked out");
                                    }
                                    // If favorite or gift message
                                    else if (classProperties.contains("tmg-live-video-react-chat-message-item")) {
                                        //System.out.println("Member gifted or faved");
                                    }
                                    // If ban message
                                    else if (classProperties.contains("tmg-live-video-ban-message")) {
                                        //System.out.println("Meetme banned member");
                                    }
                                    // If chat
                                    else {
                                        WebElement img = chatMsgs.get(i).findElement(By.cssSelector("img.tmg-live-video-react-chat-message-image"));
                                        String idImg = img.getAttribute("src");
                                        String msg = chatMsgs.get(i).findElement(By.cssSelector("span.tmg-live-video-chat-message")).getText();
                                        // If member commenting is not saved, fetch participants list and add the member
                                        if (totalViewers.get(idImg) == null) {
                                            HashMap<String, Member> newParticipants = parseParticipants(getParticipants());
                                            totalViewers.putAll(newParticipants);
                                            System.out.println("Img:" + idImg);
                                        }
                                        // If member can still not be found, fetch total viewers list
                                        if (totalViewers.get(idImg) == null) {
                                            System.out.println("Member was still not found, pulling all viewers");
                                            HashMap<String, Member> newParticipants = parseViewers(getViewers());
                                            totalViewers.putAll(newParticipants);
                                        }
                                        try {
                                            totalViewers.get(idImg).getName();
                                        } catch (Exception e) {
                                            System.out.println("ERROR: member not found. Msg: " + chatMsgs.get(i).getText());
                                        }
                                        //System.out.println(msg);
                                        //System.out.println(idImg);
                                        // Define new chat message
                                        ChatMessage chatMsg = new ChatMessage(totalViewers.get(idImg), msg);
                                        chatList.add(chatMsg);
                                        ScrollPane chatPane = (ScrollPane) ((StackPane) livePane.getChildren().get(1)).getChildren().get(0);
                                        VBox chatMessages = (VBox) chatPane.getContent();

                                        if ((chatPane.getVvalue() == 1.0)){
                                            shouldScroll = true;
                                            System.out.println("SET TO TRUE");
                                        }

                                        // Add chat message to scroll pane
                                        chatMessages.getChildren().add(chatMsg.getNodes());
                                    }

                                    //connectMsgToMember(, totalViewers);
                                    //System.out.println("TODO");
                                }
                                chatSize = chatMsgs.size();
                            }
                        }
                    });
                    //System.out.println("("+chatMsgs.size()+")");
                    try {
                        //System.out.println("Sleep");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    private EventHandler logOut() {
        EventHandler eventHandler = new EventHandler() {
            @Override
            public void handle(Event event) {
                System.out.println("Attempted to logout");
                //TODO implement logout handler
            }
        };
        return eventHandler;
    }

    private void pullData() {
        shouldPollData = true;
        Thread poll = new Thread() {
            public void run() {
                try {
                    while (shouldPollData) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                refreshLiveData();
                            }
                        });

                        // Poll data every 100 ms
                        Thread.sleep(100);
                    }
                } catch (InterruptedException v) {
                    System.out.println(v);
                }
            }
        };
        poll.start();
    }

    private JSONObject sendChat(String msg, String liveID) {
        String url = "https://api.gateway.meetme.com/web-live/client/api/sns-chat/sendText";
        JSONObject params = new JSONObject();
        params.put("groupName", liveID);
        params.put("message", msg);
        params.put("hostAppName", "meetme");
        params.put("hostAppVersion", "5.20.4");
        ArrayList<PropertyEntry> properties = new ArrayList<PropertyEntry>();
        properties.add(new PropertyEntry("accept", "application/json, text/javascript, */*; q=0.01"));
        properties.add(new PropertyEntry("accept-language", "en-US,en;q=0.9"));
        properties.add(new PropertyEntry("content-type", "application/json; charset=UTF-8"));
        properties.add(new PropertyEntry("x-parse-session-token", SESSION_TOKEN));
        properties.add(new PropertyEntry("x-requested-with", "XMLHttpRequest"));

        JSONObject result = null;
        try {
            result = (JSONObject) new JSONParser().parse(client.sendRequest("POST", url, params.toString(), properties));
            System.out.println("Message sent: " + msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void populateFavs() {
        // Clear FlowPane
        livePane.getChildren().clear();
        // Retrieve all following broadcast data
        JSONObject favs = (JSONObject) getFollowingBroadcasts("all").get("result");
        JSONArray broadCasts = (JSONArray) favs.get("broadcasts");
        ArrayList<BroadcastLabel> favBroadcasts = new ArrayList<BroadcastLabel>();

        // If at least one favored streamer is live
        if (broadCasts.size() != 0) {
            // For each favorite streamer that is live
            for (int i = 0; i < broadCasts.size(); i++) {
                // Define broadcast details
                JSONObject broadCast = (JSONObject) broadCasts.get(i);
                long currentViewers = (long) broadCast.get("currentViewers");
                long totalViewers = (long) broadCast.get("totalViewers");
                String streamDescription = "";
                if (broadCast.get("streamDescription") != null)
                    streamDescription = broadCast.get("streamDescription").toString();

                JSONObject userDetails = (JSONObject) (broadCast.get("userDetails"));
                String title = userDetails.get("firstName").toString();
                String picUrl = ((JSONObject) userDetails.get("profilePic")).get("square").toString();

                // Add a new broadcastLabel to the arraylist
                favBroadcasts.add(new BroadcastLabel(title, streamDescription, currentViewers, totalViewers, picUrl));

                // Format FlowPane
                livePane.setPadding(new Insets(10));
                livePane.setAlignment(Pos.TOP_LEFT);
                livePane.setVgap(5);
                livePane.setHgap(5);
                favBroadcasts.get(i).getLabel().setOnMouseClicked(submitBroadcastID(broadcastID = broadCast.get("objectId").toString()));
                livePane.getChildren().add(favBroadcasts.get(i).getLabel());
            }
        } else {
            Label noFaved = new Label("No Favorites are Live");
            noFaved.setId("noFavsLabel");
            livePane.setAlignment(Pos.CENTER);
            livePane.getChildren().add(noFaved);
        }
    }

    private JSONObject getFollowingBroadcasts(String gender) {
        String url = "https://api.gateway.meetme.com/web-live/client/api/sns-video/getFollowingBroadcasts";
        JSONObject params = new JSONObject();
        params.put("gender", gender);
        params.put("hostAppName", "meetme");
        params.put("hostAppVersion", "5.20.4");
        params.put("more", false);
        params.put("pageSize", 5);
        params.put("score", "0");


        ArrayList<PropertyEntry> headers = new ArrayList<PropertyEntry>();
        headers.add(new PropertyEntry("content-type", "application/json; charset=UTF-8"));
        headers.add(new PropertyEntry("x-parse-session-token", SESSION_TOKEN));
        JSONObject favs = null;
        try {
            favs = (JSONObject) new JSONParser().parse(client.sendRequest("POST", url, params.toString(), headers));
        } catch (Exception e) {
        }
        return favs;
    }

    private EventHandler submitBroadcastID(TextField liveID) {
        return new EventHandler() {
            @Override
            public void handle(Event event) {
                handleSubmit(liveID.getText());
            }
        };
    }

    private EventHandler submitBroadcastID(String liveID) {
        EventHandler eventHandler = new EventHandler() {
            @Override
            public void handle(Event event) {
                handleSubmit(liveID);
            }
        };
        return eventHandler;
    }

    private void handleSubmit(String liveID) {
        System.out.println("Text: " + liveID);
        broadcastMetadata = getBroadcastMetadata(broadcastID = liveID);
        populateLive();

        Task<Void> getViewers = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                totalViewers = parseViewers(getViewers());
                return null;
            }
        };

        new Thread(getViewers).start();


        getLiveChat(broadcastID);
        //pullData();
    }

    private JSONObject getBroadcastMetadata(String broadCastID) {
        // Setup GET request method parameters
        String metadataURL = "https://api.gateway.meetme.com/web-live/client/apiAuth/getBroadcastMetadata";
        ArrayList<PropertyEntry> getHeaders = new ArrayList<PropertyEntry>();

        // Create JSON Object and define all parameters
        JSONObject requestParams = new JSONObject();
        requestParams.put("broadcastId", broadCastID);
        requestParams.put("installationId", INSTALLATION_ID);
        requestParams.put("parseToken", SESSION_TOKEN);
        requestParams.put("hostAppName", "meetme");
        requestParams.put("hostAppVersion", "5.20.4");
        requestParams.put("authToken", ACCESS_TOKEN);

        // Add all appropriate request headers
        getHeaders.add(new PropertyEntry("content-type", "application/json; charset=UTF-8"));
        getHeaders.add(new PropertyEntry("accept", "application/json, text/javascript, */*; q=0.01"));
        getHeaders.add(new PropertyEntry("referer", "https://api.gateway.meetme.com/web-live/view/" + broadCastID));

        // Retrieve metadata of the broadcast
        JSONObject broadcastMetadata = new JSONObject();
        try {
            broadcastMetadata = (JSONObject) new JSONParser().parse(client.sendRequest("GET", metadataURL, requestParams.toJSONString(), getHeaders));
            broadcastMetadata = (JSONObject) broadcastMetadata.get("result");
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        JSONObject result = (JSONObject) broadcastMetadata;
        result = (JSONObject) ((JSONObject) result.get("broadcast")).get("result");
        System.out.println("Viewers: " + result.get("currentViewers") + " out of " + result.get("totalViewers"));

        return broadcastMetadata;
    }

    private JSONObject getOauthInfo() {
        // Setup GET request method parameters
        String oauthURL = "https://ssl.meetme.com/mobile/oauthIdentifier";
        ArrayList<PropertyEntry> getHeaders = new ArrayList<PropertyEntry>();

        // Add all appropriate request headers
        getHeaders.add(new PropertyEntry("accept", "application/json, text/javascript, */*; q=0.01"));
        getHeaders.add(new PropertyEntry("accept-language", "en-US,en;q=0.9"));
        getHeaders.add(new PropertyEntry("referer", "https://beta.meetme.com"));
        getHeaders.add(new PropertyEntry("x-device", "phoenix/screen_extra_small,52f81d1b-471d-438f-87b8-abefd1993839,5.20.4"));

        // To retrieve an access token, we need to pass a token.
        // Send a get request and oauthIdentifier gives us this token.
        JSONObject oauthIdentifier = new JSONObject();
        try {
            oauthIdentifier = (JSONObject) new JSONParser().parse(client.sendRequest("GET", oauthURL, null, getHeaders));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        // Setup POST request method parameters
        String authURL = "https://auth.gateway.meetme.com/oauth/token";
        ArrayList<PropertyEntry> postHeaders = new ArrayList<PropertyEntry>();
        String postParams = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange";
        postParams += "&subject_token=" + oauthIdentifier.get("token");
        postParams += "&subject_token_type=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Asession";

        // Authorization header encoding. "meetme:secret" was found through MeetMe HTTPS examination
        // More info: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization
        String basicAuthorHeader = "Basic " + Base64.getEncoder().encodeToString("meetme:secret".getBytes());

        // Add all appropriate request headers
        postHeaders.add(new PropertyEntry("content-type", "application/x-www-form-urlencoded"));
        postHeaders.add(new PropertyEntry("Authorization", basicAuthorHeader));
        postHeaders.add(new PropertyEntry("accept", "*/*"));
        postHeaders.add(new PropertyEntry("accept-language", "en-US,en;q=0.9"));
        postHeaders.add(new PropertyEntry("referer", "https://beta.meetme.com/"));
        postHeaders.add(new PropertyEntry("x-device", "web"));
        postHeaders.add(new PropertyEntry("x-brand", "meetme"));

        // Send POST request and return oauth data
        JSONObject returnVal = new JSONObject();
        try {
            returnVal = (JSONObject) new JSONParser().parse(client.sendRequest("POST", authURL, postParams, postHeaders));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        return returnVal;

    }

    private JSONObject getUserInfo() {
        // Setup GET request method parameters
        String meURL = "https://video-api.meetme.com/1/users/me";
        ArrayList<PropertyEntry> getHeaders = new ArrayList<PropertyEntry>();

        // Add all appropriate request headers
        getHeaders.add(new PropertyEntry("content-type", "application/json; charset=UTF-8"));
        getHeaders.add(new PropertyEntry("accept", "application/json, text/javascript, */*; q=0.01"));
        getHeaders.add(new PropertyEntry("accept-language", "en-US,en;q=0.9"));
        getHeaders.add(new PropertyEntry("x-device", "phoenix/screen_extra_small,52f81d1b-471d-438f-87b8-abefd1993839,5.20.4"));

        // Create JSON Object and define all request parameters
        JSONObject requestParams = new JSONObject();
        requestParams.put("_method", "GET");
        requestParams.put("_ApplicationId", "sns-video");
        requestParams.put("_ClientVersion", "js2.4.0");
        requestParams.put("_InstallationId", INSTALLATION_ID);
        requestParams.put("_SessionToken", ACCESS_TOKEN);

        // To retrieve an access token, we need to pass a token.
        // Sending a get request and oauthIdentifier gives us this token.
        JSONObject meData = new JSONObject();
        try {
            meData = (JSONObject) new JSONParser().parse(client.sendRequest("GET", meURL, requestParams.toString(), getHeaders));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        return meData;
    }

    private JSONObject getParticipants() {
        // Setup POST request method parameters
        String url = "https://api.gateway.meetme.com/web-live/client/api/sns-chat/getParticipantsByName";
        ArrayList<PropertyEntry> postHeaders = new ArrayList<PropertyEntry>();

        // Add all appropriate request headers
        postHeaders.add(new PropertyEntry("content-type", "application/json; charset=utf-8"));
        postHeaders.add(new PropertyEntry("accept", "application/json, text/javascript, */*; q=0.01"));
        postHeaders.add(new PropertyEntry("x-parse-session-token", SESSION_TOKEN));

        // Create JSON Object and define all request parameters
        JSONObject requestParams = new JSONObject();
        requestParams.put("groupName", broadcastID);
        requestParams.put("hostAppName", "meetme");
        requestParams.put("hostAppVersion", "5.20.4");

        // Define return object and send request
        JSONObject participants = new JSONObject();
        try {
            participants = (JSONObject) new JSONParser().parse(client.sendRequest("GET", url, requestParams.toString(), postHeaders));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        return participants;
    }

    private JSONObject getViewers() {
        // Setup POST request method parameters
        String url = "https://api.gateway.meetme.com/web-live/client/api/sns-video/getAllViewersByDiamondSort";
        ArrayList<PropertyEntry> postHeaders = new ArrayList<PropertyEntry>();

        // Add all appropriate request headers
        postHeaders.add(new PropertyEntry("content-type", "application/json; charset=utf-8"));
        postHeaders.add(new PropertyEntry("accept", "application/json, text/javascript, */*; q=0.01"));
        postHeaders.add(new PropertyEntry("x-parse-session-token", SESSION_TOKEN));

        // Retrieve how many people are in the live
        JSONObject metadata = (JSONObject) ((JSONObject) getBroadcastMetadata(broadcastID).get("broadcast")).get("result");
        long totalViewers = (long) metadata.get("totalViewers");

        // Create JSON Object and define all request parameters
        JSONObject requestParams = new JSONObject();
        requestParams.put("broadcastId", broadcastID);
        requestParams.put("hostAppName", "meetme");
        requestParams.put("hostAppVersion", "5.20.4");
        // This parameter defines the max amount of viewers to return.
        requestParams.put("pageSize", totalViewers);
        requestParams.put("score", "0");

        // Define return object and send request
        JSONObject viewers = new JSONObject();
        try {
            viewers = (JSONObject) new JSONParser().parse(client.sendRequest("POST", url, requestParams.toString(), postHeaders));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        return viewers;
    }

    private HashMap<String, Member> parseViewers(JSONObject viewersData) {
        HashMap<String, Member> viewers = new HashMap<String, Member>();
        viewersData = (JSONObject) viewersData.get("result");
        JSONArray broadcastMembers = (JSONArray) viewersData.get("broadcastViewers");
        int iterator = 2;
        while (iterator != 0) {
            for(int i = 0; i < broadcastMembers.size(); i++) {
                JSONObject memberData = (JSONObject) broadcastMembers.get(i);
                JSONObject userDetails = (JSONObject) memberData.get("userDetails");
                String fullName = userDetails.get("firstName").toString();
                if(userDetails.get("lastName") != null)
                    fullName += userDetails.get("lastName").toString();
                String objectId = ((JSONObject) userDetails.get("user")).get("objectId").toString();
                String picURL = null;
                String idURL = null;
                if(userDetails.get("profilePic") != null) {
                    picURL = ((JSONObject) userDetails.get("profilePic")).get("large").toString();
                    idURL = ((JSONObject) userDetails.get("profilePic")).get("square").toString();
                }
                Member mem = new Member(fullName, objectId, idURL);
                mem.setIdImageURL(idURL);

                //System.out.println("Viewer added: "+fullName);
                viewers.put(idURL, mem);
            }
            broadcastMembers = (JSONArray) viewersData.get("broadcastFans");
            iterator--;
        }

        System.out.println("Viewer num: "+viewers.size());

        return viewers;
    }

    private HashMap<String, Member> parseParticipants(JSONObject viewersData) {
        //TODO condense with parseViewers, if possible
        HashMap<String, Member> viewers = new HashMap<String, Member>();
        viewersData = (JSONObject) viewersData.get("result");
        JSONArray broadcastMembers = (JSONArray) viewersData.get("participants");

        for(int i = 0; i < broadcastMembers.size(); i++) {
            JSONObject memberData = (JSONObject) broadcastMembers.get(i);
            String fullName = memberData.get("firstName").toString();
            if(memberData.get("lastName") != null)
                fullName += memberData.get("lastName").toString();
            String objectId = ((JSONObject) memberData.get("user")).get("objectId").toString();
            String picURL = null;
            String idURL = null;
            if(memberData.get("profilePic") != null) {
                picURL = ((JSONObject) memberData.get("profilePic")).get("large").toString();
                idURL = ((JSONObject) memberData.get("profilePic")).get("square").toString();
            }
            Member mem = new Member(fullName, objectId, idURL);
            mem.setIdImageURL(idURL);
            if(viewers.get(mem.getIdImageURL()) == null) {
                System.out.println("Viewer added: "+fullName+" "+idURL+" "+mem.getIdImageURL());
            }
            viewers.put(mem.getIdImageURL(), mem);
        }

        //System.out.println("Viewer num: "+viewers.size());

        return viewers;
    }


    public void refreshLiveData() {
        JSONObject result = (JSONObject) ((JSONObject) broadcastMetadata.get("broadcast")).get("result");
        broadcastMetadata = getBroadcastMetadata(result.get("objectId").toString());
        // Parse broadcast metadata

        // Define elements to be refreshed
        Label viewers = (Label) ((FlowPane) livePane.getChildren().get(0)).getChildren().get(1);

        // Refresh elements
        viewers.setText(result.get("currentViewers").toString() + " : " + result.get("totalViewers").toString());
    }

    public void populateLive() {
        // Parse broadcast metadata
        JSONObject result = (JSONObject) ((JSONObject) broadcastMetadata.get("broadcast")).get("result");

        // Initial setup of live pane
        livePane.getChildren().clear();
        livePane.setPrefWidth(((GridPane) livePane.getParent()).getWidth());
        livePane.setPrefHeight(((GridPane) livePane.getParent()).getWidth());
        livePane.setAlignment(Pos.TOP_LEFT);
        livePane.setVgap(10);

        // Setup chat
        chatList = new ArrayList<ChatMessage>();

        // Define livePane elements
        FlowPane infoBar = new FlowPane();
        Rectangle chatOverlay = new Rectangle();
        StackPane liveContainer = new StackPane();
        ScrollPane chatPane = new ScrollPane();
        VBox chat = new VBox();
        TextField chatBar = new TextField();

        // Define infoBar elements
        ImageView eye = new ImageView(new Image(getClass().getResourceAsStream("images/eye32x32.png")));
        Label viewers = new Label(result.get("currentViewers").toString() + " / " + result.get("totalViewers").toString());

        // Set element IDs for css styling
        infoBar.setId("infoBar");
        chatPane.setId("chatPane");
        chatBar.setId("chatBar");
        eye.setId("viewersIcon");
        viewers.setId("viewersCount");

        // Setup info bar
        infoBar.setPrefWidth(livePane.getWidth() - 40);
        infoBar.setAlignment(Pos.CENTER_LEFT);
        infoBar.setHgap(5);

        // Setup chat pane
        chat.setSpacing(10);
        chat.heightProperty().addListener(observable -> {
            double oldVvalue = chatPane.getVvalue();
            chatPane.setVmax(chat.getHeight());
            chatPane.setVvalue(chatPane.getVmax());
            //System.out.println("Height changed. Should scroll? "+shouldScroll+" Vmax: "+chatPane.getVmax());
            if(chat.getHeight() > chatPane.getHeight()) {
                if(shouldScroll || firstScroll) {
                    smoothScroll(chatPane, -100);
                    firstScroll = false;
                }
            }
        });
        chatPane.setVmax(chat.getHeight());
        chatPane.setPrefWidth(livePane.getWidth() - 40);
        chatPane.setPrefHeight(livePane.getHeight() - 100);
        chatOverlay.setHeight(livePane.getHeight() - 100);
        chatOverlay.setWidth(livePane.getWidth() - 40);
        chatOverlay.setOpacity(0.0);
        chatOverlay.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                //System.out.println("Triggered: " + event.getDeltaY());
                // DeltaY should always be +-40 for developer mouse
                smoothScroll(chatPane, event.getDeltaY());
            }
        });

        // Setup chat bar, used to send messages
        chatBar.setPrefWidth(livePane.getWidth() - 40);
        chatBar.setPromptText("Say something...");
        chatBar.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    sendChat(chatBar.getText(), broadcastID);
                    chatBar.clear();
                }
            }
        });

        // Add all infoBar elements to info bar
        infoBar.getChildren().addAll(eye, viewers);

        // Setup chat pane with overlay
        chatPane.setContent(chat);
        liveContainer.getChildren().addAll(chatPane, chatOverlay);

        // Add all elements to parent
        livePane.getChildren().addAll(infoBar, liveContainer, chatBar);
    }

    private void smoothScroll(ScrollPane chatPane, double amount) {
        Thread animateScroll = new Thread() {
            @Override
            public void run() {
                shouldScroll = false;
                    for (double i = 1; i < Math.abs(amount); i++) {
                        double finalI = i;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                // If scroll down
                                if (amount < 0) {
                                    chatPane.setVvalue(chatPane.getVvalue() + 2);
                                    System.out.println("Scrolling down: " + chatPane.getVvalue());
                                }
                                // If scroll up
                                else {
                                    chatPane.setVvalue(chatPane.getVvalue() - 2);
                                    System.out.println("Scrolling up: " + chatPane.getVvalue());
                                }
                            }
                        });

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // If scrollbar is at top or bottom of pane, exit loop
                        if (chatPane.getVvalue() == 1.0 || chatPane.getVvalue() == 0.0) {
                            System.out.println("Breaking");
                            break;
                        }
                    }
                    System.out.println("Broken");
                }
        };

        animateScroll.start();
    }

    public void populateControls() {
        // Define elements
        TextField liveID = new TextField();
        Button submit = new Button("Submit");
        Button logout = new Button("Logout");

        // TODO: add menu functionality
        logout.setDisable(true);

        // Setup elements
        liveID.setPromptText("Broadcast ID");
        submit.setOnAction(submitBroadcastID(liveID));
        logout.setOnAction(logOut());

        // Populate VBox and format
        menu.setAlignment(Pos.CENTER);
        menu.setVgap(5);
        menu.setHgap(5);
        menu.getChildren().addAll(liveID, submit, logout);
    }

    public void populateProfile() {
        // Image properties
        int imgWidth = 200;
        int imgHeight = imgWidth;
        int roundMargin = imgWidth;
        int shadowOffset = 5;

        // Define elements
        Rectangle imageContainer = new Rectangle(imgWidth, imgHeight);
        ImagePattern pattern = new ImagePattern(new Image(user.getImgUrl(), imgWidth, imgHeight, false, false));
        Label info = new Label(user.getName() + ", " + user.getAge());
        Label about_me = new Label(user.getBio());

        // Set ids for css styling
        imageContainer.setId("userPic");
        info.setId("userInfo");
        about_me.setId("userBio");

        // Round profile pic with use of an ImagePattern
        imageContainer.setArcWidth(roundMargin);
        imageContainer.setArcHeight(roundMargin);
        imageContainer.setFill(pattern);

        // Add drop shadow to image
        DropShadow shadow = new DropShadow(20, Color.BLACK);
        shadow.setOffsetX(shadowOffset);
        shadow.setOffsetY(shadowOffset);
        imageContainer.setEffect(shadow);

        // Populate VBox and format
        //profileInfo.setSpacing(0);
        profileInfo.setAlignment(Pos.TOP_CENTER);
        profileInfo.getChildren().addAll(imageContainer, info, about_me);
    }

    public void setClientAndLoginInfo(HTTPClient client, JSONObject loginResponse) {
        this.client = client;
        this.loginResponse = loginResponse;

        populateScene();
    }

    public void setStageAndSetupListeners(Stage stage, Scene loginScene, Scene liveScene, Controller_login loginController) {
        this.stage = stage;
        this.loginScene = loginScene;
        this.liveScene = liveScene;
        this.loginController = loginController;
    }
}