package com.mphasis.tse.exception;


public class EmptyFileException extends RuntimeException{
    public EmptyFileException(String message){
        super(message);
    }
}
