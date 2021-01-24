package browser;


import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

public class ChatMessage {
    private Member member;

    private String msg;

    public ChatMessage(Member member, String msg) {
        this.member = member;
        this.msg = msg;
    }

    public Member getMember() {
        return member;
    }

    public String getMsg() {
        return msg;
    }

    public HBox getNodes() {
        // Define elements
        int margin = 35;
        HBox container = new HBox();
        VBox textContainer = new VBox();
        Rectangle imageContainer = new Rectangle(margin, margin);
        ImagePattern profPic = new ImagePattern(new Image(member.getImgUrl(),margin, margin, false, false));
        Label name = new Label(member.getName());
        Label message = new Label(getMsg());

        // Set ids for CSS styling
        name.setId("chat-user");
        message.setId("chat-message");

        // Format elements
        imageContainer.setArcWidth(margin);
        imageContainer.setArcHeight(margin);
        imageContainer.setFill(profPic);
        container.setSpacing(5);

        // Add all elements and return HBox
        textContainer.getChildren().addAll(name, message);
        container.getChildren().addAll(imageContainer, textContainer);
        return  container;
    }
}
