import * as React from 'react';
import { styled, alpha } from '@mui/material/styles';
import Menu, { MenuProps } from '@mui/material/Menu';
import { IconButton } from '@mui/material';
import MenuItem from '@mui/material/MenuItem';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import Divider from '@mui/material/Divider';
import LogoutIcon from '@mui/icons-material/Logout';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import DeleteIcon from '@mui/icons-material/Delete';
import { useNavigate } from "react-router-dom";
import { useStompClient } from "react-stomp-hooks";
import MessengerService from '../../../services/MessengerService';
import Chat from '../../../models/Chat';
import User from '../../../models/User';
import { SecuredPages } from '../../../util/Constants';
import UserSelector from './UserSelector';
import { ID } from '../../../util/Types';
import { Permissions } from '../../../models/Permissions';
import { UserFilter } from '../../../services/UserService';

const StyledMenu = styled((props: MenuProps) => (
  <Menu
    elevation={0}
    anchorOrigin={{
      vertical: 'bottom',
      horizontal: 'right',
    }}
    transformOrigin={{
      vertical: 'top',
      horizontal: 'right',
    }}
    {...props}
  />
))(({ theme }) => ({
  '& .MuiPaper-root': {
    borderRadius: 6,
    marginTop: theme.spacing(1),
    minWidth: 180,
    color: 'rgb(55, 65, 81)',
    boxShadow:
      'rgb(255, 255, 255) 0px 0px 0px 0px, rgba(0, 0, 0, 0.05) 0px 0px 0px 1px, rgba(0, 0, 0, 0.1) 0px 10px 15px -3px, rgba(0, 0, 0, 0.05) 0px 4px 6px -2px',
    '& .MuiMenu-list': {
      padding: '4px 0',
    },
    '& .MuiMenuItem-root': {
      '& .MuiSvgIcon-root': {
        fontSize: 18,
        color: theme.palette.text.secondary,
        marginRight: theme.spacing(1.5),
        ...theme.applyStyles('dark', {
          color: 'inherit',
        }),
      },
      '&:active': {
        backgroundColor: alpha(
          theme.palette.primary.main,
          theme.palette.action.selectedOpacity,
        ),
      },
    },
    ...theme.applyStyles('dark', {
      color: theme.palette.grey[300],
    }),
  },
}));

export default function ChatRoomMenu({ chat, user, permissions, closeChat }:
  { chat: Chat, user?: User | null, permissions?: string[], closeChat?: () => void }) {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);
  const [userSelectorOpen, setUserSelectorOpen] = React.useState(false);
  const [action, setAction] = React.useState('');
  const [userSelectorFilter, setUserSelectorFilter] = React.useState<UserFilter>({});
  const stompClient = useStompClient();
  const navigate = useNavigate();
  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
    setUserSelectorOpen(false);
  };

  const addUsersToChat = React.useCallback(async (selectedUsers: ID[]) => {
    await MessengerService.addUsersToChat(chat.id, selectedUsers);
    handleClose();
  }, [chat.id]);

  const deleteUsersFromChat = React.useCallback(async (selectedUsers: ID[]) => {
    await MessengerService.deleteUsersFromChat(chat.id, selectedUsers);
    handleClose();
  }, [chat.id]);

  const addParticipants = React.useCallback(() => {
    setAction('add');
    const userFilter: UserFilter = { chatId: chat.id, exclude: true };
    setUserSelectorFilter(userFilter);
    setUserSelectorOpen(true);
  }, [chat.id]);

  const deleteParticipants = React.useCallback(async () => {
    setAction('delete');
    const userFilter: UserFilter = { chatId: chat.id, exclude: false };
    setUserSelectorFilter(userFilter);
    setUserSelectorOpen(true);
  }, [chat.id]);

  const onUsersSelected = React.useCallback((selectedUsers: ID[]) => {
    if (action === 'add') {
      addUsersToChat(selectedUsers);
    } else if (action === 'delete') {
      deleteUsersFromChat(selectedUsers);
    }
  }, [action, addUsersToChat, deleteUsersFromChat]);

  const leaveChat = React.useCallback(async () => {
    if (!user?.id) return;
    if (!chat.private) {
      stompClient?.unsubscribe(`/topic/chats/${chat.id}/messages`);
    }
    await MessengerService.deleteUsersFromChat(chat.id, [user.id]);
    handleClose();
    closeChat?.();
    navigate(SecuredPages.CHATS_PAGE, { replace: true });
  }, [chat.id, chat.private, user?.id, stompClient, navigate, closeChat]);

  const deleteChat = React.useCallback(async () => {
    if (!user?.id || !chat.id) return;
    if (!chat.private) {
      stompClient?.unsubscribe(`/topic/chats/${chat.id}/messages`);
    }
    // TODO: Should we notify others that chat is deleted?
    await MessengerService.deleteChat(chat.id);
    handleClose();
    closeChat?.();
    navigate(SecuredPages.CHATS_PAGE, { replace: true });
  }, [chat.id, chat.private, user?.id, stompClient, navigate, closeChat]);

  return (
    <div>
      <IconButton onClick={handleClick}>
        <MoreVertIcon fontSize="medium"
        />
      </IconButton>
      <StyledMenu
        id="demo-customized-menu"
        slotProps={{
          list: {
            'aria-labelledby': 'demo-customized-button',
          },
        }}
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
      >
        {
          !chat.private &&
          (permissions?.includes(Permissions.Chat.ADMIN) || permissions?.includes(Permissions.Chat.User.ADD)) &&
          <MenuItem onClick={addParticipants} disableRipple>
            <PersonAddIcon />
            Add participants
          </MenuItem>
        }
        {
          !chat.private &&
          permissions?.includes(Permissions.Chat.ADMIN) &&
          <MenuItem onClick={deleteParticipants} disableRipple>
            <PersonAddIcon />
            Delete participants
          </MenuItem>
        }
        <Divider sx={{ my: 0.5 }} />
        <MenuItem onClick={leaveChat} disableRipple>
          <LogoutIcon />
          Leave chat
        </MenuItem>
        {
          !chat.private &&
          permissions?.includes(Permissions.Chat.ADMIN) &&
          <MenuItem onClick={deleteChat} disableRipple>
            <DeleteIcon />
            Delete chat
          </MenuItem>
        }
      </StyledMenu>
      <UserSelector isOpen={userSelectorOpen} onResult={onUsersSelected} onCancel={handleClose} filter={userSelectorFilter}/>
    </div >
  );
}
