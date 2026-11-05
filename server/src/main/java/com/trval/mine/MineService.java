package com.trval.mine;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MineService {

    public MineProfile profile() {
        return new MineProfile(
                new UserProfile("旅行用户"),
                List.of(
                        new PlanRecord("1", "广东省 / 广州市", "440100", "上海市 / 上海市", "310100", "3000", "2", "2026-11-01"),
                        new PlanRecord("2", "北京市 / 北京市", "110100", "四川省 / 成都市", "510100", "5000", "4", "2026-10-18"),
                        new PlanRecord("3", "浙江省 / 杭州市", "330100", "陕西省 / 西安市", "610100", "2600", "3", "2026-09-06")
                )
        );
    }
}
