package com.trval.mine;

import com.trval.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MineController {

    private final MineService mineService;

    public MineController(MineService mineService) {
        this.mineService = mineService;
    }

    @GetMapping("/mine")
    public ApiResponse<MineProfile> mine() {
        return ApiResponse.ok(mineService.profile());
    }
}
