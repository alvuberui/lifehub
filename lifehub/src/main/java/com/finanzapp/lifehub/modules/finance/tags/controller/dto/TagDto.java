package com.finanzapp.tags.controller.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagDto {

    private Long id;
    private String name;
    private String color;
}
