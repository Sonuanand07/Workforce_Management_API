package com.railse.hiring.workforcemgmt.mapper;

import com.railse.hiring.workforcemgmt.dto.TaskActivityDto;
import com.railse.hiring.workforcemgmt.model.TaskActivity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ITaskActivityMapper {
    TaskActivityDto modelToDto(TaskActivity model);
    TaskActivity dtoToModel(TaskActivityDto dto);
    List<TaskActivityDto> modelListToDtoList(List<TaskActivity> models);
}