package com.finanzapp.tags.service.domain;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTagRequest {

    private String name;
    private String color;
}
