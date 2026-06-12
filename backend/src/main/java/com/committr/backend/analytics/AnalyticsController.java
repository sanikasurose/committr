package com.committr.backend.analytics;

import com.committr.backend.dto.analytics.CodingHoursDto;
import com.committr.backend.dto.analytics.CommitFrequencyDto;
import com.committr.backend.dto.analytics.ContributorShareDto;
import com.committr.backend.dto.analytics.LanguageTrendDto;
import com.committr.backend.dto.analytics.PrVelocityDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repos/{id}/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/contributors")
    public List<ContributorShareDto> contributors(@PathVariable Long id) {
        return analyticsService.getContributors(id);
    }

    @GetMapping("/language-trend")
    public List<LanguageTrendDto> languageTrend(@PathVariable Long id) {
        return analyticsService.getLanguageTrend(id);
    }

    @GetMapping("/coding-hours")
    public CodingHoursDto codingHours(@PathVariable Long id) {
        return analyticsService.getCodingHours(id);
    }

    @GetMapping("/commit-frequency")
    public List<CommitFrequencyDto> commitFrequency(@PathVariable Long id) {
        return analyticsService.getCommitFrequency(id);
    }

    @GetMapping("/pr-velocity")
    public List<PrVelocityDto> prVelocity(@PathVariable Long id) {
        return analyticsService.getPrVelocity(id);
    }
}
