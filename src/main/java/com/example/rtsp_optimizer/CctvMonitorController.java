package com.example.rtsp_optimizer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CctvMonitorController {

    private final CctvConfig cctvConfig;

    public CctvMonitorController(CctvConfig cctvConfig) {
        this.cctvConfig = cctvConfig;
    }

    @GetMapping("/monitor")
    public String monitor(Model model) {
        model.addAttribute("cctvStreams", cctvConfig.getStreams());
        return "monitor";
    }
}