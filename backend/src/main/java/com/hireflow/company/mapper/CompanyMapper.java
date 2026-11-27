package com.hireflow.company.mapper;

import com.hireflow.company.dto.CompanyResponse;
import com.hireflow.company.entity.Company;
import org.mapstruct.Mapper;

@Mapper
public interface CompanyMapper {

    CompanyResponse toResponse(Company company);
}
