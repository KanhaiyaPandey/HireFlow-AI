package com.hireflow.recruitment.mapper;

import com.hireflow.recruitment.dto.JobPostingResponse;
import com.hireflow.recruitment.dto.JobResponse;
import com.hireflow.recruitment.entity.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface JobMapper {

    @Mapping(target = "applicationCount", source = "applicationCount")
    JobResponse toResponse(Job job, long applicationCount);

    @Mapping(target = "companyName", source = "companyName")
    JobPostingResponse toPosting(Job job, String companyName);
}
