package canter.test_assignment.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SingleToDo {
    int id;
    String todo;
    String completed;
    int userId;

    @Override
    public String toString() {
        return String.format("Id: %d\nToDo: %s\nCompleted: %s\nUser Id: %d\n", id, todo, completed, userId);
    }
}
