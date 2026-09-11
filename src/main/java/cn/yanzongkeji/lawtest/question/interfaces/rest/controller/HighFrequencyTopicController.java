package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.application.highfrequency.HighFrequencyTopicCommand;
import cn.yanzongkeji.lawtest.question.application.highfrequency.HighFrequencyTopicService;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.HighFrequencyTopicRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.HighFrequencyTopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "今日高频考点", description = "管理员维护考点，客户端随机读取一条")
public class HighFrequencyTopicController {
    private final HighFrequencyTopicService topics;

    @GetMapping("/high-frequency-topics/random")
    @Operation(summary = "随机获取一个今日高频考点")
    public ResponseEntity<HighFrequencyTopicResponse> random() {
        var topic = topics.random();
        if (topic == null)
            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(HighFrequencyTopicResponse.from(topic));
    }

    @GetMapping("/admin/high-frequency-topics")
    @Operation(summary = "查询全部高频考点")
    public java.util.List<HighFrequencyTopicResponse> list() {
        return topics.list().stream().map(HighFrequencyTopicResponse::from).toList();
    }

    @PostMapping("/admin/high-frequency-topics")
    @Operation(summary = "录入高频考点")
    public ResponseEntity<HighFrequencyTopicResponse> create(@RequestBody HighFrequencyTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(HighFrequencyTopicResponse.from(topics.create(toCommand(request))));
    }

    @PutMapping("/admin/high-frequency-topics/{id}")
    @Operation(summary = "更新高频考点")
    public HighFrequencyTopicResponse update(@PathVariable long id, @RequestBody HighFrequencyTopicRequest request) {
        return HighFrequencyTopicResponse.from(topics.update(id, toCommand(request)));
    }

    @DeleteMapping("/admin/high-frequency-topics/{id}")
    @Operation(summary = "删除高频考点")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        topics.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static HighFrequencyTopicCommand toCommand(HighFrequencyTopicRequest request) {
        return new HighFrequencyTopicCommand(request.title(), request.summary(), request.category());
    }
}
