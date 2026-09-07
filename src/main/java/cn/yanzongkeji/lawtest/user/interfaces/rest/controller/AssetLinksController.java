package cn.yanzongkeji.lawtest.user.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.user.infrastructure.configuration.AuthProperties;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssetLinksController {
    private final AuthProperties.Android android;

    public AssetLinksController(AuthProperties properties) {
        this.android = properties.android();
    }

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, ?>>> assetLinks() {
        Map<String, Object> target = Map.of("namespace", "android_app", "package_name", android.packageName(),
                "sha256_cert_fingerprints", android.certificateSha256());
        return ResponseEntity.ok(List.of(Map.of("relation", List.of("delegate_permission/common.handle_all_urls",
                "delegate_permission/common.get_login_creds"), "target", target)));
    }
}
