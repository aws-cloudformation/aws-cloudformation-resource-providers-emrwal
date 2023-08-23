package software.amazon.emr.walworkspace;

import software.amazon.awssdk.services.emrwal.model.Tag;

public class TagHelper {


    public static Tag toSDKTag (software.amazon.emr.walworkspace.Tag tag) {
        return Tag.builder().key(tag.getKey()).value(tag.getValue()).build();
    }

    public static software.amazon.emr.walworkspace.Tag toResourceModelTag (Tag tag) {
        //On server side, if the value is "", it will return null, there is revert null to ""
        String key = tag.key();
        String value = tag.value() == null ? "": tag.value();
        return new software.amazon.emr.walworkspace.Tag(key, value);
    }
}
