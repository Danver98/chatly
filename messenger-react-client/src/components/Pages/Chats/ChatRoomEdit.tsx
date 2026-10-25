import * as React from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import Chat from '../../../models/Chat';
import User from '../../../models/User';
import { ID } from '../../../util/Types';
import MessengerService from '../../../services/MessengerService';
import { Avatar, Box, Chip, FormControlLabel, List, ListItem, ListItemAvatar, ListItemButton, ListItemText, Switch } from '@mui/material';
import InputAdornment from '@mui/material/InputAdornment';
import { styled } from '@mui/material/styles';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import SearchIcon from '@mui/icons-material/Search';
import Divider from '@mui/material/Divider';
import { toast } from 'sonner';
import { Permissions } from '../../../models/Permissions';
import "./Chats.css";

import PersonAddAlt1Icon from '@mui/icons-material/PersonAddAlt1';
import { Snackbar, Tooltip, IconButton } from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { BROKEN_IMAGE_URL } from '../../../util/Constants';
import { stringToColor } from '../../../util/avatar';
import { useFetchUsers } from './FetchData';
import { getUserNetworkStatus } from '../../../util/Functions';
import UserProfileDialog from '../../Dialogs/UserProfileDialog';


const GenerateChatInvitationLink = ({ userId, chatId }: { userId: ID, chatId: ID }) => {

    const [link, setLink] = React.useState('');
    const [open, setOpen] = React.useState(false);

    const copyLink = async () => {
        if (!link) return;
        navigator.clipboard.writeText(link);
        setOpen(true); // Show notification
    };

    const generateLink = async () => {
        const invitationLink = await MessengerService.generateChatInvitationLink(chatId);
        setLink(invitationLink);
    };

    return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, marginTop: 1 }}>
            <Button variant="contained" onClick={generateLink} size="small">
                Generate Invitation Link
            </Button>
            <TextField
                variant="outlined"
                size="small"
                value={link}
                slotProps={{
                    input: {
                        readOnly: true,
                        endAdornment: (
                            <InputAdornment position="end">
                                <>
                                    <Tooltip title="Copy to clipboard">
                                        <IconButton onClick={copyLink} size="small">
                                            <ContentCopyIcon />
                                        </IconButton>
                                    </Tooltip>

                                    {/* Visual Feedback Notification */}
                                    <Snackbar
                                        open={open}
                                        onClose={() => setOpen(false)}
                                        autoHideDuration={2000}
                                        message="Link copied to clipboard!"
                                    />
                                </>
                            </InputAdornment>
                        )
                    },
                }}
            />
        </Box>
    );
};

const VisuallyHiddenInput = styled('input')({
    clipPath: 'inset(50%)',
    height: 1,
    overflow: 'hidden',
    position: 'absolute',
    bottom: 0,
    left: 0,
    whiteSpace: 'nowrap',
    width: 1,
});


function InputFileUpload({ text, readOnly, handleChange, sx }: { text: string, readOnly: boolean, handleChange?: (e: any) => void, sx?: object }) {
    return (
        <Button
            component="label"
            role={undefined}
            variant="contained"
            disabled={readOnly}
            tabIndex={-1}
            startIcon={<CloudUploadIcon />}
            sx={sx}
        >
            {text}
            <VisuallyHiddenInput
                type="file"
                onChange={handleChange}
                multiple
            />
        </Button>
    );
}

function MembersList({chatId, parentCloseHandler}: { chatId: ID, parentCloseHandler: () => void }) {
    const [search, setSearch] = React.useState<string|null>(null);
    const [userDialogOpen, setUserDialogOpen] = React.useState(false);
    const [selectedUser, setSelectedUser] = React.useState<User|null>(null);
    const filter = React.useMemo(() => {
        return {
            chatId,
            search
        }
    }, [chatId, search]);
    const sorting = React.useMemo(() => {
        return {
            networkStatus: 'DESC'
        }
    }, []);
    const navigation = React.useMemo(() => {
        return {
            maxPageSize: 50,
            cursor: null
        }
    }, []);
    const { users: members, isLoading, hasMore, loadMore } = useFetchUsers(filter, sorting, navigation);

    const observerRef = React.useRef<IntersectionObserver | null>(null);
    const [lastElement, setLastElement] = React.useState<HTMLElement | null>(null);

    const usersRef = React.useRef(members);
    React.useEffect(() => {
        usersRef.current = members;
    }, [members]);

    const loadMoreRef = React.useRef(loadMore);
    React.useEffect(() => {
        loadMoreRef.current = loadMore;
    }, [loadMore]);

    /** Intersection Observer */
    React.useEffect(() => {
        if (isLoading) return;
        if (observerRef.current) observerRef.current.disconnect();
        observerRef.current = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting && hasMore) {
                // Triggering hook method to load more data
                loadMoreRef.current();
            }
        });
        if (lastElement) observerRef.current.observe(lastElement);
        
        return () => observerRef.current?.disconnect();
    }, [isLoading, hasMore, lastElement]);

    const now = new Date();

    const onItemClick = React.useCallback((user: User) => {
        setUserDialogOpen(true);
        setSelectedUser(user);
    }, []);

    const onUserDialogClose = React.useCallback((closeAll: boolean = false) => {
        setUserDialogOpen(false);
        setSelectedUser(null);
        if (closeAll) parentCloseHandler();
    }, [parentCloseHandler]);

    const onSearchChange = React.useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        const s = e.target.value ? e.target.value : null;
        if (!s || s.length === 0) setSearch(s);
        if (s && s.length < 3) return;
        setSearch(s);
    }, []);

    return (
        <Box
            className='chat-room-edit__membersList'
            sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
                marginTop: 1, width: '100%'}}
        >
            <Box className='chat-room-edit__membersList__header'
                sx={{ display: 'flex', width: '80%', justifyContent: 'space-between', flexGrow: 1, margin: 0 }}
            >
                <TextField
                    fullWidth
                    type="search"
                    variant="outlined"
                    placeholder="Search"
                    value={search}
                    size="small"
                    onChange={onSearchChange}
                    slotProps={{
                        input: {
                            disableUnderline: true,
                            endAdornment: (
                            <InputAdornment position="end">
                                <SearchIcon />
                            </InputAdornment>
                            ),
                        },
                    }}
                    sx={{
                        mt: 1,
                        mr: 1,
                        ml: 1,
                        "& .MuiFilledInput-root": {
                            borderTopLeftRadius: "25px",
                            borderTopRightRadius: "25px",
                            borderBottomLeftRadius: "25px",
                            borderBottomRightRadius: "25px",

                        },
                    }}
                />
                <IconButton
                    onClick={() => {}}
                    title="Add member"
                >
                    <PersonAddAlt1Icon />
                </IconButton>
            </Box>
            <List sx={{ width: '100%', bgcolor: 'background.paper' }}>
                {
                    !members && <p>No members</p>
                }
                {   members &&
                    members.map((member: User, index: number) => {
                        const name = member.name ?? '';
                        const surname = member.surname ?? '';
                        const altImgText = name[0].toUpperCase() + ' ' + surname[0].toUpperCase();
                        const statusLabel = getUserNetworkStatus(member, now);
                        return (
                            <ListItem
                                key={member.id}
                                ref={index === members.length - 1 ? setLastElement : undefined}
                            >
                                <ListItemButton
                                    onClick={() => onItemClick(member)}
                                    sx={{
                                        borderRadius: 2,
                                        px: 2,
                                        '&:hover': {
                                            backgroundColor: 'action.hover',
                                        },
                                        '&:active': {
                                            backgroundColor: 'action.selected',
                                        },
                                    }} 
                                >
                                    <ListItemAvatar >
                                        <Avatar
                                            src={member.avatar ?? BROKEN_IMAGE_URL}
                                            alt={altImgText}
                                            children={altImgText}
                                            sx={{
                                                width: 50,
                                                height: 50,
                                                marginRight: '4px',
                                                bgcolor: stringToColor(name + ' ' + surname)
                                            }}
                                        />
                                    </ListItemAvatar>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            flexDirection: 'column',
                                            justifyContent: 'center',
                                        }}
                                    >
                                        <ListItemText
                                            sx={{
                                                mb: 0,
                                            }}
                                            primary={member.name + ' ' + member.surname}
                                        >
                                        </ListItemText>
                                        <ListItemText
                                            sx={{
                                                mt: 0,
                                            }}
                                            secondary={statusLabel}
                                            slotProps={{
                                                secondary: {
                                                    sx: { color: statusLabel === 'Online' ? 'info.main' : 'text.secondary' }
                                                }
                                            }}
                                        >
                                        </ListItemText>
                                    </Box>
                                </ListItemButton>
                            </ListItem>
                        )
                    })
                }
            </List>
            <UserProfileDialog open={userDialogOpen} user={selectedUser} onClose={onUserDialogClose} />
        </Box>
    );
}

export default function ChatRoomEdit({ isOpen = false, chatId, user, permissions, onResult = () => { } }:
    { isOpen: boolean, chatId: ID, user?: User | null, permissions?: string[], onResult?: () => void }) {
    const [open, setOpen] = React.useState(isOpen);
    const [readOnly, setReadOnly] = React.useState(true);
    const [chat, setChat] = React.useState<Chat | null>(null);
    const [newImageUrl, setNewImageUrl] = React.useState<string | null>(null);
    const [imageFile, setImageFile] = React.useState<File | null>(null);

    const handleClose = () => {
        setReadOnly(true);
        setOpen(false);
        setNewImageUrl(null);
        setImageFile(null);
        onResult()
    };

    React.useEffect(() => {
        setOpen(isOpen);
    }, [isOpen]);

    React.useEffect(() => {
        const fetchChat = async (id: ID) => {
            const data = await MessengerService.getChat(id, user?.id);
            setChat(data);
        }
        if (open) {
            fetchChat(chatId);
        }
    }, [chatId, user?.id, open]);

    const handleChange = React.useCallback(<K extends keyof Chat>(value: any, field: K) => {
        if (!chat) return;
        const chatCopy: Chat = chat.copy();
        chatCopy[field] = value;
        setChat(chatCopy);
    }, [chat]);

    const handleFileChange = React.useCallback((event: any) => {
        const file = event.target.files[0];
        if (file) {
            // Create a temporary local URL for the selected file
            const localUrl = URL.createObjectURL(file);
            setNewImageUrl(localUrl);
            setImageFile(file);
        }
    }, []);

    const handleSubmit = (event: any) => {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const formJson = Object.fromEntries((formData as any).entries());
        const email = formJson.email;
        console.log(email);
        handleClose();
    };

    const saveHandler = async () => {
        if (!readOnly && chat) {
            const chatCopy: Chat = chat.copy();
            if (imageFile) {
                const chatImageUrl = await MessengerService.sendAttachment(imageFile as File, chatId, user?.id);
                if (chatImageUrl == null) {
                    return toast.error(`Failed to upload "${imageFile.name}" resource to the server`);
                }
                chatCopy.avatar = chatImageUrl;
            }
            await MessengerService.updateChatPatch(chatCopy);
            handleClose();
        }
        setReadOnly(!readOnly);
    }

    return (
        <React.Fragment>
            <Dialog open={open} onClose={handleClose}
                slotProps={{
                    paper: {
                        sx: {
                            borderRadius: 5,
                            width: '40%',
                            height: '80%',
                        }
                    }
                }}
            >
                <DialogContent className="chat-room-edit">
                    <form onSubmit={handleSubmit} id="subscription-form" className="chat-room-edit__form">
                        <Avatar
                            alt="Chat avatar image"
                            src={newImageUrl ?? (chat && chat?.avatar ? chat.avatar : undefined)}
                            sx={{
                                width: 180,
                                height: 180,
                                marginBottom: 1
                            }}
                        />
                        <InputFileUpload
                            text="Upload avatar"
                            readOnly={readOnly}
                            handleChange={handleFileChange}
                            sx={{
                                mb: 1
                            }}
                        />
                        <TextField
                            required
                            margin="dense"
                            id="name"
                            name="name"
                            label="Name"
                            variant="outlined"
                            disabled={readOnly}
                            value={chat?.name ?? ''}
                            slotProps={{ inputLabel: { shrink: !!chat?.name } }}
                            onChange={(event: any) => {
                                handleChange(event.target.value, 'name');
                            }}
                            sx={{
                                width: '50%'
                            }}
                            className="chat-room-edit__NameField"
                        />
                        <FormControlLabel
                            control={<Switch
                                checked={chat?.canAddUsers ?? false}
                                disabled={readOnly}
                                onChange={(event: any) => {
                                    handleChange(event.target.checked, 'canAddUsers');
                                }}
                            />}
                            label="Users can add members"
                        />
                        {
                            (chat?.canAddUsers || permissions?.includes(Permissions.Chat.ADMIN)) &&
                            <GenerateChatInvitationLink chatId={chatId} userId={user?.id} />
                        }
                    </form>
                    <Divider 
                        sx={{
                            mt: 1
                        }}
                    >
                        <Chip label="Members" size="small" />
                    </Divider>
                    <MembersList chatId={chatId} parentCloseHandler={handleClose} />
                </DialogContent>
                <DialogActions className="chat-room-edit--dialogActions"
                    sx={{
                        margin: 1
                    }}
                >
                    <Button onClick={handleClose}>Cancel</Button>
                    <Button
                        type={readOnly ? "button" : "submit"}
                        size="small"
                        variant="contained"
                        disabled={!permissions?.includes(Permissions.Chat.ADMIN)}
                        onClick={() => { saveHandler() }}>{
                            readOnly ? 'Edit' : 'Save'
                        }
                    </Button>
                </DialogActions>
            </Dialog>
        </React.Fragment>
    );
}
