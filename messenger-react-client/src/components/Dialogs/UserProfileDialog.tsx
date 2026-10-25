import React from 'react';
import {
    Dialog, Box, Typography, Avatar, IconButton, Stack, Button, List,
    ListItem, ListItemIcon, ListItemText, Divider,
} from '@mui/material';

import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CloseIcon from '@mui/icons-material/Close';
import ChatIcon from '@mui/icons-material/Chat';
import NotificationsOffIcon from '@mui/icons-material/NotificationsOff';
import CallIcon from '@mui/icons-material/Call';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import LinkIcon from '@mui/icons-material/Link';
import BarChartIcon from '@mui/icons-material/BarChart';
import MicIcon from '@mui/icons-material/Mic';
import GroupIcon from '@mui/icons-material/Group';
import ShareIcon from '@mui/icons-material/Share';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import BlockIcon from '@mui/icons-material/Block';

import User from '../../models/User';
import { getUserNetworkStatus } from '../../util/Functions';
import { stringToColor } from '../../util/avatar';
import { BROKEN_IMAGE_URL } from '../../util/Constants';
import { useAuthStore } from '../../stores/authStore';
import { useChatStore } from '../../stores/chatStore';

interface UserProfileDialogProps {
    open: boolean;
    user?: User | null;
    onClose: () => void;
}

/**
 * Public wrapper. Handles the "no user / not open" cases BEFORE any hooks run,
 * so the inner component can use hooks safely with a non-null user.
 */
export default function UserProfileDialog({ open, user, onClose }: UserProfileDialogProps) {
    if (!open || !user) return null;
    return <UserProfileDialogInner user={user} onClose={onClose} />;
}

interface InnerProps {
    user: User;
    onClose: (closeAll?: boolean) => void;
}

function UserProfileDialogInner({ user, onClose }: InnerProps) {
    const currentUserId = useAuthStore((s) => s.currentLoggedUser?.id);
    const openPrivateChatWith = useChatStore((s) => s.openPrivateChatWith);

    const handleMessage = React.useCallback(() => {
        if (!currentUserId || user.id == null) return;
        onClose(true); // close the profile first — chat opens underneath
        openPrivateChatWith(currentUserId, user.id);
    }, [currentUserId, user.id, onClose, openPrivateChatWith]);

    // Reusable Action Button for the top row.
    const ActionButton = ({
        icon, label, onClick, disabled,
    }: {
        icon: React.ReactNode;
        label: string;
        onClick?: () => void;
        disabled?: boolean;
    }) => (
        <Button
            variant="outlined"
            onClick={onClick}
            disabled={disabled}
            sx={{
                flex: 1,
                flexDirection: 'column',
                py: 1.5,
                textTransform: 'none',
                color: 'text.primary',
                borderColor: 'divider',
                '&:hover': { backgroundColor: 'action.hover', borderColor: 'divider' },
            }}
        >
            <Box sx={{ mb: 0.5, display: 'flex', alignItems: 'center' }}>{icon}</Box>
            <Typography variant="caption" sx={{ fontWeight: 500 }}>{label}</Typography>
        </Button>
    );

    const name = user.name ?? '';
    const surname = user.surname ?? '';
    const altImgText = (name[0] ?? '').toUpperCase() + ' ' + (surname[0] ?? '').toUpperCase();
    const statusLabel = getUserNetworkStatus(user, new Date());

    // Don't let a user "message themselves" — disable in that case.
    const isSelf = currentUserId != null && user.id === currentUserId;

    return (
        <Dialog
            open
            onClose={onClose}
            maxWidth="xs"
            fullWidth
            slotProps={{
                paper: {
                    sx: {
                        borderRadius: 3,
                        maxHeight: '90vh',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                },
            }}
        >
            <Box sx={{ display: 'flex', justifyContent: 'space-between', p: 1, flexShrink: 0 }}>
                <IconButton onClick={onClose}><ArrowBackIcon /></IconButton>
                <IconButton onClick={onClose}><CloseIcon /></IconButton>
            </Box>

            <Box sx={{ overflowY: 'auto', flex: 1 }}>
                {/* PROFILE INFO */}
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', px: 3, pb: 2 }}>
                    <Avatar
                        src={user.avatar ?? BROKEN_IMAGE_URL}
                        alt={altImgText}
                        sx={{
                            width: 100, height: 100, mb: 2,
                            bgcolor: stringToColor(name + ' ' + surname),
                        }}
                    >
                        {altImgText}
                    </Avatar>
                    <Typography variant="h6" fontWeight="bold">
                        {user.name + ' ' + user.surname}
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{ color: statusLabel === 'Online' ? 'info.main' : 'text.secondary' }}
                    >
                        {statusLabel}
                    </Typography>
                </Box>

                {/* ACTION BUTTONS */}
                <Stack direction="row" spacing={1} sx={{ px: 2, pb: 3 }}>
                    <ActionButton
                        icon={<ChatIcon color="action" />}
                        label="Message"
                        onClick={handleMessage}
                        disabled={isSelf || !currentUserId}
                    />
                    <ActionButton icon={<NotificationsOffIcon color="action" />} label="Unmute" />
                    <ActionButton icon={<CallIcon color="action" />} label="Call" />
                    <ActionButton icon={<MoreHorizIcon color="action" />} label="More" />
                </Stack>

                {/* CONTACT DETAILS */}
                <Box sx={{ px: 2, pb: 1 }}>
                    <Typography variant="body1" fontWeight="medium">{user.phone}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>Mobile</Typography>

                    <Typography variant="body1" color="primary.main" fontWeight="medium">{user.username}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>Username</Typography>

                    <Typography variant="body1" fontWeight="medium">{user.birthday}</Typography>
                    <Typography variant="body2" color="text.secondary">Birthday</Typography>
                </Box>

                <Divider sx={{ mt: 2 }} />

                {/* SHARED MEDIA / STATS */}
                <List disablePadding>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40 }}><LinkIcon /></ListItemIcon>
                        <ListItemText primary="1 shared link" />
                    </ListItem>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40 }}><BarChartIcon /></ListItemIcon>
                        <ListItemText primary="2 polls" />
                    </ListItem>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40 }}><MicIcon /></ListItemIcon>
                        <ListItemText primary="3 voice messages" />
                    </ListItem>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40 }}><GroupIcon /></ListItemIcon>
                        <ListItemText primary="4 groups in common" />
                    </ListItem>
                </List>

                <Divider />

                {/* ACCOUNT ACTIONS */}
                <List disablePadding sx={{ pb: 2 }}>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40 }}><ShareIcon /></ListItemIcon>
                        <ListItemText primary="Share this contact" />
                    </ListItem>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40 }}><EditIcon /></ListItemIcon>
                        <ListItemText primary="Edit contact" />
                    </ListItem>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40 }}><DeleteIcon /></ListItemIcon>
                        <ListItemText primary="Delete contact" />
                    </ListItem>
                    <ListItem button>
                        <ListItemIcon sx={{ minWidth: 40, color: 'error.main' }}>
                            <BlockIcon color="error" />
                        </ListItemIcon>
                        <ListItemText primary="Block user" sx={{ color: 'error.main' }} />
                    </ListItem>
                </List>
            </Box>
        </Dialog>
    );
}