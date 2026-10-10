import { useState,  useRef } from "react";
import Message from "../../../models/Message";
import AttachFileIcon from '@mui/icons-material/AttachFile';
import { Box, IconButton, TextareaAutosize } from "@mui/material";
import ClearIcon from '@mui/icons-material/Clear';
import ReplyIcon from '@mui/icons-material/Reply';
import SendIcon from '@mui/icons-material/Send';

/**
 * Component for sending messages
 */
export default function MessageSender({ handleSubmit, replyMessage, setReplyMessage }:
    { handleSubmit: (event: any) => any, replyMessage: Message | null,
        setReplyMessage: (message: Message | null) => void }) {
    const fileUpload = useRef<any>(null);
    const textArea = useRef<HTMLTextAreaElement | null>(null);
    const [selectedFiles, setSelectedFiles] = useState<string[]>([]);
    const [hasTextInput, setHasTextInput] = useState(false);
    
    const fileInputChanged = (event: any) => {
        setSelectedFiles((prev) => [...prev, event.target.value])
    }

    const clearFileList = (event: any) => {
        fileUpload.current.value = null;
        setSelectedFiles([]);
    }

    const clearInput = (event: any) => {
        if (textArea.current) {
            textArea.current.value = '';
        }
    }

    const onTextChange = () => {
        const value = textArea.current?.value ?? '';
        setHasTextInput(value.trim().length > 0);
    }

    const onSubmit = async (event: any) => {
        await handleSubmit(event);
        clearFileList(event);
        clearInput(event);
    }

    const hasInput = hasTextInput || selectedFiles.length > 0;

    return (
        <Box className="chat-room-message-sender"
            sx={{ display: 'flex', flexDirection: 'column', width: '100%', justifyContent: 'center', padding: '8px 0px 8px 16px', border: '1px solid rgb(213, 218, 217)', borderRadius: '15px', backgroundColor: 'common.white' }}
        >
            {
                replyMessage &&
                <Box sx={{ display: 'flex', flexDirection: 'row', width: '100%', alignItems: 'center', pb: 1 }} className="chat-room-message-sender__replyContainer tw-flex tw-flex-col">
                    <Box className="replyIcon"
                        sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 40, flexShrink: 0}}
                    >
                        <ReplyIcon
                            color="primary"
                        />
                    </Box>
                    <Box
                        sx={{ display: 'flex', flexDirection: 'column', width: '100%', alignItems: 'flex-start', flex: 1, minWidth: 0, ml: 1 }} 
                        className="chat-room-message-sender__reply"
                    >
                        <Box sx={{color: 'primary.main', fontWeight: 'bold', cursor: 'pointer'}} className="chat-room-message-sender__replyTo">
                            Reply to {replyMessage.getAuthorFullName()}
                        </Box>
                        <Box className="chat-room-message-sender__replyMessageText truncate"
                            sx={{
                                minWidth: 0,
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                width: '100%'
                            }}
                        >
                            {replyMessage.getDataText()}
                        </Box>
                    </Box>
                    <Box sx={{cursor: 'pointer', pr: 1}} className="chat-room-message-sender__replyClearIcon">
                        <ClearIcon
                            color="primary"
                            onClick={() => setReplyMessage(null)}
                        />
                    </Box>
                </Box>
            }
            <form
                method="POST"
                onSubmit={onSubmit}
                className="chat-room-message-sender__FormContainer"
            >
                <Box className="chat-room-message-sender__Form"
                    sx={{ display: 'flex', flex: 'auto', justifyContent: 'center', alignItems: 'center' }}
                >
                    <IconButton
                        color="primary"
                        onClick={() => { fileUpload.current.click() }}
                        sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 40, height: 40, flexShrink: 0}}
                    >
                        <AttachFileIcon />
                    </IconButton>
                    <input
                        name="fileData"
                        type="file"
                        hidden
                        ref={fileUpload}
                        onChange={(event) => { fileInputChanged(event) }}
                    />
                    <Box className="chat-room-message-sender__TextArea-container"
                        sx={{ display: 'flex', flexDirection: 'column', flex: 1, minWidth: 0 }}
                    >
                        <TextareaAutosize
                            aria-label="minimum height"
                            name="messageData"
                            minRows={3}
                            maxRows={3}
                            placeholder="Enter your message"
                            className="chat-room-message-sender__TextArea"
                            ref={textArea}
                            onChange={onTextChange}
                            style={{ border: 'none', outline: 'none', resize: 'none' }}
                        />
                        {
                            selectedFiles && selectedFiles.length > 0 &&
                            <div>
                                {
                                    selectedFiles.join(', ')
                                }
                                <IconButton
                                    onClick={clearFileList}
                                >
                                    <ClearIcon />
                                </IconButton>

                            </div>
                        }
                    </Box>
                    {
                        hasInput &&
                        <IconButton
                            color="primary"
                            type="submit"
                            size="medium"
                            title="Send message"
                            sx={{mr: 1}}
                        >
                            <SendIcon />
                        </IconButton>
                    }
                </Box>
            </form>
        </Box>
    )
}