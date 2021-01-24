package browser;

import javafx.scene.image.Image;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Member {
    private boolean isUser = false;
    private long memberID;
    private String objectID;
    private long age;
    private String name;
    private String bio;
    private String profImgURL;
    private String idImageURL;

    public Member() {}

    public Member(String name, String objectID, String profImgURL) {
        this.name = name;
        this.objectID = objectID;
        this.profImgURL = profImgURL;
    }

    public final void setAsUser() {
        isUser = true;
    }

    public boolean isUser() {
        return isUser;
    }

    public void parseMemInfo(JSONObject member) {
        name = member.get("first_name")+" "+member.get("last_name");
        bio = (String) member.get("about_me");
        age = (Long) member.get("age");
        memberID = (Long) member.get("member_id");
        profImgURL = "https://images.meetmecdna.com/"+member.get("picture");
    }

    public String getImgUrl() {
        return profImgURL;
    }

    public String getName() {
        return name;
    }

    public String getBio() {
        return bio;
    }

    public long getAge() {
        return age;
    }

    public long getMemberID() {
        return memberID;
    }

    public void setIdImageURL(String url) {
        idImageURL = url;
    }

    public String getIdImageURL() {
        return idImageURL;
    }
}
