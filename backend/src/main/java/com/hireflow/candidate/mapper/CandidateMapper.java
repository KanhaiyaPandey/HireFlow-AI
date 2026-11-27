package com.hireflow.candidate.mapper;

import com.hireflow.candidate.dto.CandidateResponse;
import com.hireflow.candidate.entity.Candidate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CandidateMapper {

    @Mapping(target = "hasAccount", expression = "java(candidate.hasAccount())")
    CandidateResponse toResponse(Candidate candidate);
}
