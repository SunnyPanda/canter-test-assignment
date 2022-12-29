package canter.test_assignment.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ToDo {
    int id;
    String todo;
    String completed;
    int userId;
}
