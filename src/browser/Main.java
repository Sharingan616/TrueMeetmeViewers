package browser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        // Misc app variables
        int appWidth = 720;
        int appHeight = 480;
        String title = "MeetMe Assistant";

        // Setup loaders for each xml file
        FXMLLoader loader_login = new FXMLLoader(getClass().getResource("login.fxml"));
        FXMLLoader loader_live = new FXMLLoader(getClass().getResource("live.fxml"));

        // Define all stage scenes
        Scene loginScene = new Scene(loader_login.load(), appWidth, appHeight);
        Scene liveScene = new Scene(loader_live.load(), appWidth, appHeight);

        // Final setup and scene setting
        primaryStage.setTitle(title);
        primaryStage.setScene(loginScene);

        // Define all controllers
        Controller_login controller_login = loader_login.getController();
        Controller_live controller_live = loader_live.getController();

        // Pass stage and scene info to controllers
        controller_login.setStageAndSetupListeners(primaryStage, loginScene, liveScene, controller_live);
        controller_live.setStageAndSetupListeners(primaryStage, loginScene, liveScene, controller_login);

        // Show first stage
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
