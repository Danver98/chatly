import { forwardRef, } from "react";
import Chat from "../../../models/Chat";
import { MessageDataType, MessageType } from "../../../models/Message";
import "./Chats.css";
import "../../../_styles/Common.css";
import { Avatar } from "@mui/material";
import { BROKEN_IMAGE_URL } from "../../../util/Constants";
import { stringToColor } from "../../../util/avatar";

const LastMessage = ({ chat }: { chat: Chat }) => {
    const messageType = chat.lastMessage?.type;
    if (messageType && messageType !== MessageType.CHAT) {
        return (
            <div className="chat-list-item__lastMessage">
                <span className="chat-list-item__lastMessage-service">{ chat.lastMessage?.getDataText() }</span>
            </div>
        )
    }
    const messageDataType = chat.lastMessage?.data?.type;
    if (messageDataType === MessageDataType.JOIN_LINK) {
        return (
            <div className="chat-list-item__lastMessage">
                <span className="chat-list-item__lastMessage-author">{chat.lastMessage?.getAuthorFullName()}</span>
                <span className="chat-list-item__lastMessage-text">{ ': ' + 'link' }</span>
            </div>
        )
    }
    return (
        <div className="chat-list-item__lastMessage">
            <span className="chat-list-item__lastMessage-author">{chat.lastMessage?.getAuthorFullName()}</span>
            <span className="chat-list-item__lastMessage-text">{ ': ' + chat.lastMessage?.getDataText() }</span>
        </div>
    )
}

const ItemBody = ({ chat }: { chat: Chat }) => {
    const parts = chat.name?.split(' ');
    const altImage = parts ? parts[0][0] + (parts.length > 1 ? parts[1][0] : parts[0][1]) : 'Chat';
    return (
        <>
            <Avatar
                className="chat-room-page-header__avatar"
                src={chat.avatar ?? BROKEN_IMAGE_URL}
                sx={{
                    width: 70,
                    height: 70,
                    bgcolor: stringToColor(chat.name ?? '')
                }}
                alt={altImage.toUpperCase()}
                children={altImage.toUpperCase()}
            />
            <div className="chat-list-item__info">
                <div className="chat-list-item__nameDate">
                    <div className="chat-list-item__name">
                        <b>{chat.name}</b>
                    </div>
                    <div className="chat-list-item__date">
                        <span>{chat.getTime()?.toLocaleTimeString("ru")}</span>
                        <br />
                        <span>{chat.getTime()?.toLocaleDateString("ru")}</span>
                    </div>
                </div>
                {
                    chat.lastMessage &&
                    <div className="chat-list-item__messageInfo">
                        <LastMessage chat={chat} />
                        {
                            chat.unreadMsgCount &&
                            <div className="chat-list-item__messageCounter">
                                <div className="chat-list-item__messageCounter-round">
                                    { chat.unreadMsgCount }
                                </div>
                            </div>
                        }
                    </div>
                }
            </div>
        </>
    )
}

const ChatListItem = forwardRef(({ chat, isLast, clickHandler }:
    { chat: Chat, isLast: boolean, clickHandler?: (chat: Chat) => void }, ref?: any) => {
    if (isLast) {
        return (
            <li
                key={chat.id}
                ref={ref}
                onClick={() => { clickHandler?.(chat) }}
                className="chat-list-item"
            >
                <ItemBody chat={chat} />
            </li>
        )
    }
    return (
        <li
            key={chat.id}
            onClick={() => { clickHandler?.(chat) }}
            className="chat-list-item"
        >
            <ItemBody chat={chat} />
        </li>
    )
})

const ChatsList = forwardRef(({ chats, itemClickHandler }: { chats: Chat[], itemClickHandler: (chat: Chat) => any }, ref: any) => {
    if (chats == null || chats.length === 0) {
        return (
            <>
                No chats!
            </>
        )
    }

    const listItems = chats.map((chat, index) =>
        <ChatListItem key={chat.id} chat={chat} isLast={index === chats.length - 1} clickHandler={itemClickHandler} ref={ref} />
    )
    return (
        <ul className="chat-list">{listItems}</ul>
    )
})

export default ChatsList;