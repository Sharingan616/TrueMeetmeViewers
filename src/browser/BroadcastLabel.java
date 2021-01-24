package browser;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.awt.*;

public class BroadcastLabel {

    private StackPane label = new StackPane();
    private String title;
    private String description;
    private long currentViews;
    private long totalViews;
    private String img;
    private boolean isTopBadge = false;
    private boolean isVs = false;
    private boolean isDating = false;
    private boolean isBlindDating = false;

    public BroadcastLabel(String title, String description, long currentViews, long totalViews, String img) {
        this.title = title;
        this.description = description;
        this.currentViews = currentViews;
        this.totalViews = totalViews;
        this.img = img;
    }

    public StackPane getLabel() {

        // Define StackPane elements
        ImageView profileImg = new ImageView(new Image(getImg()));
        VBox text = new VBox();
        Label title = new Label(getTitle());
        Label desc =  new Label(getDescription());

        // Format elements
        title.getStyleClass().add("streamLabelTitle");
        desc.getStyleClass().add("streamLabelDesc");
        profileImg.setFitWidth(150);
        profileImg.setFitHeight(150);
        text.setAlignment(Pos.BOTTOM_CENTER);
        text.getChildren().addAll(title, desc);
        label.setMaxWidth(profileImg.getFitWidth());
        label.getChildren().addAll(profileImg, text);
        return label;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getCurrentViews() {
        return currentViews;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public String getImg() {
        return img;
    }

    public boolean isTopBadge() {
        return isTopBadge;
    }

    public void setTopBadge() {
        this.isTopBadge = true;
    }

    public boolean isVs() {
        return isVs;
    }

    public void setVs(boolean vs) {
        isVs = vs;
    }

    public boolean isDating() {
        return isDating;
    }

    public void setDating(boolean dating) {
        isDating = dating;
    }

    public boolean isBlindDating() {
        return isBlindDating;
    }

    public void setBlindDating(boolean blindDating) {
        isBlindDating = blindDating;
    }
}
