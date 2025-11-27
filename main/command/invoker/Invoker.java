package main.command.invoker;

import main.command.command.Command;

/**
 * Command Pattern: Invoker
 * Command 객체를 실행하는 역할
 */
public class Invoker {
    private Command command;
    
    /**
     * 실행할 Command를 설정합니다.
     */
    public void setCommand(Command command) {
        this.command = command;
    }
    
    /**
     * 설정된 Command를 실행합니다.
     */
    public void executeCommand() {
        if (command != null) {
            command.execute();
        }
    }
}


