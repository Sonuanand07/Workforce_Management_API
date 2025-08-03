package com.railse.hiring.workforcemgmt.mapper;

import com.railse.hiring.workforcemgmt.dto.TaskCommentDto;
import com.railse.hiring.workforcemgmt.model.TaskComment;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ITaskCommentMapper {
    TaskCommentDto modelToDto(TaskComment model);
    TaskComment dtoToModel(TaskCommentDto dto);
    List<TaskCommentDto> modelListToDtoList(List<TaskComment> models);
}