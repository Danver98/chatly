import * as React from 'react';
import { styled, alpha } from '@mui/material/styles';
import Menu, { MenuProps } from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ReplyIcon from '@mui/icons-material/Reply';
import DeleteIcon from '@mui/icons-material/Delete';
import Chat from '../../../models/Chat';
import User from '../../../models/User';
import Message from '../../../models/Message';

export const MENU_ACTIONS = {
    REPLY: 'Reply',
    DELETE: 'Delete'
};

export type MenuAction = typeof MENU_ACTIONS[keyof typeof MENU_ACTIONS];

const StyledMenu = styled((props: MenuProps) => (
  <Menu
    elevation={0}
    anchorOrigin={{
      vertical: 'bottom',
      horizontal: 'left',
    }}
    transformOrigin={{
      vertical: 'top',
      horizontal: 'left',
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

export default function MessageMenu({ user, message, anchorPosition, onAction, handleClose }:
  { user: User | null, chat: Chat, message: Message | null, anchorPosition: object | null,
    onAction: (actionId: string, item: any) => void, handleClose: () => void }) {
  const open = Boolean(anchorPosition !== null);

  const replyToMessage = React.useCallback(() => {
    onAction(MENU_ACTIONS.REPLY, message);
  }, [message, onAction]);

  const deleteMessage = React.useCallback(async () => {
    onAction(MENU_ACTIONS.DELETE, message);
  }, [message, onAction]);

  return (
    <div className="message-menu">
      <StyledMenu
        id="message-menu"
        slotProps={{
          list: {
            'aria-labelledby': 'message-menu',
          },
        }}
        anchorReference="anchorPosition"
        anchorPosition={anchorPosition ?? undefined}
        open={open}
        onClose={handleClose}
      >
        <MenuItem onClick={replyToMessage} disableRipple>
            <ReplyIcon />
            Reply
        </MenuItem>
        {
            user?.id === message?.author?.id &&
            <MenuItem onClick={deleteMessage} disableRipple>
                <DeleteIcon />
                Delete
            </MenuItem>
        }
      </StyledMenu>
    </div >
  );
}
