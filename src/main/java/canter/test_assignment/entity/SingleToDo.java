package canter.test_assignment.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SingleToDo {
    int id;
    String todo;
    String completed;
    int userId;
}
