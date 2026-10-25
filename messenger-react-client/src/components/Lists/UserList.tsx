import {
    Avatar, Box, Button, Checkbox, Dialog, FormControlLabel, InputAdornment,
    List, ListItem, ListItemAvatar, ListItemText, Switch, TextField,
} from "@mui/material";
import SearchIcon from '@mui/icons-material/Search';
import User from "../../models/User";
import { forwardRef, useCallback, useMemo, useState } from "react";
import "./UserList.css";
import { toast } from 'sonner';
import CircularProgress from '@mui/material/CircularProgress';

import { useFetchUsers } from "../Pages/Chats/FetchData";
import { INavigation, UserFilter } from "../../services/UserService";
import { stringToColor } from "../../util/avatar";
import { getUserNetworkStatus } from "../../util/Functions";
import { BROKEN_IMAGE_URL } from "../../util/Constants";


const SearchBar = ({ onChange }: { onChange: (value: string) => any }) => (
    <TextField
        id="standard-basic"
        label="Enter user credentials"
        variant="standard"
        fullWidth
        margin="normal"
        slotProps={{
            input: {
                endAdornment: (
                    <InputAdornment position="end">
                        <SearchIcon />
                    </InputAdornment>
                ),
            },
        }}
        onChange={(e) => onChange(e.target.value)}
    />
);

const UserList = forwardRef(
    (
        { users, checked, handleToggle, multiSelect }: {
            users: User[];
            checked: number[];
            handleToggle: (value: number) => any;
            multiSelect?: boolean | null;
        },
        ref?: any,
    ) => {
        if (users == null || users.length === 0) {
            return <>No users found!</>;
        }
        const now = new Date();
        return (
            <Box>
                <List>
                    {users.map((user: User, index) => {
                        const name = user.name ?? '';
                        const surname = user.surname ?? '';
                        const altImgText = name[0].toUpperCase() + ' ' + surname[0].toUpperCase();
                        const statusLabel = getUserNetworkStatus(user, now);
                        return (
                            <ListItem
                                key={user.id}
                                ref={index === users.length - 1 ? ref : null}
                                secondaryAction={
                                    <Checkbox
                                        disabled={!multiSelect && checked.length > 0 && !checked.includes(index)}
                                        checked={checked.includes(index)}
                                        onChange={() => handleToggle(index)}
                                    />
                                }
                            >
                                <ListItemAvatar>
                                    <Avatar
                                        src={user.avatar ?? BROKEN_IMAGE_URL}
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
                                        primary={user.name + ' ' + user.surname}
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
                            </ListItem>
                        )
                    })}
                </List>
            </Box>
        );
    },
);

export interface UserSelectionProps {
    user?: User | null;
    open: boolean;
    onClose: () => any;
    requestFilter?: object;
    onResult: (elements: any[], params?: any | null) => any;
}

/**
 * Public wrapper: mounts the inner dialog only when open, so useFetchUsers
 * doesn't fetch while the dialog is closed. Also resets internal state on
 * each open (unmount/remount) — which is what the old useEffects were doing.
 */
export default function UserSelection(props: UserSelectionProps) {
    if (!props.open) return null;
    return <UserSelectionInner {...props} />;
}

function UserSelectionInner({ user, onClose, requestFilter, onResult }: UserSelectionProps) {
    const [search, setSearch] = useState<string | null>(null);
    const [checked, setChecked] = useState<number[]>([]);
    const [multiSelect, setMultiSelect] = useState(false);
    const [chatName, setChatName] = useState<string | null>(null);

    // Memoize filter / sorting / navigation so useFetchUsers' effect
    // does not re-run on every render.
    const filter: UserFilter = useMemo(
        () => ({ ...(requestFilter ?? {}), search } as UserFilter),
        [requestFilter, search],
    );
    const sorting = useMemo(() => ({ networkStatus: 'DESC' }), []);
    const navigation: INavigation = useMemo(
        () => ({ maxPageSize: 50, cursor: null }),
        [],
    );

    const { users, isLoading, hasMore, loadMore } = useFetchUsers(
        filter,
        sorting,
        navigation,
    );

    const handleToggle = useCallback(
        (position: number) => {
            if (!multiSelect && checked.length > 1) return;
            setChecked((prev) => {
                const currentIndex = prev.indexOf(position);
                if (currentIndex === -1) return [...prev, position];
                const next = [...prev];
                next.splice(currentIndex, 1);
                return next;
            });
        },
        [multiSelect, checked.length],
    );

    const handleClose = useCallback(() => {
        // State is discarded by unmount (wrapper returns null when closed),
        // so we only notify the parent here.
        onClose();
    }, [onClose]);

    const handleOk = useCallback(() => {
        if (multiSelect && (chatName == null || chatName === '')) {
            toast.warning(`You'd set a chat name!`);
            return;
        }
        const ids = checked.map((position) => users[position].id);
        onClose();
        onResult(ids, { chatName, multiSelect });
    }, [users, checked, chatName, multiSelect, onResult, onClose]);

    // Simple IntersectionObserver wired to loadMore. The hook itself guards
    // against concurrent/duplicate loadMore calls, so no local "hasMore" or
    // "isLoading" checks are needed here.
    const lastElementRef = useCallback(
        (node: Element | null) => {
            if (!node) return;
            const observer = new IntersectionObserver((entries) => {
                if (entries[0].isIntersecting) loadMore();
            });
            observer.observe(node);
            return () => observer.disconnect();
        },
        [loadMore],
    );

    return (
        <Dialog open onClose={handleClose} fullWidth maxWidth="sm"
            slotProps={{
                paper: {
                    sx: {
                        borderRadius: 3,
                        height: '90vh',
                        maxHeight: '90vh',
                    },
                },
            }}
        >
            {
                isLoading && (!users || users.length === 0)
                    ? <Box
                        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                        <CircularProgress />
                      </Box>
                    :
                    <div className="participant-selection-popup-content-wrapper">
                        <FormControlLabel
                            control={
                                <Switch
                                    checked={!multiSelect}
                                    onChange={(e) => setMultiSelect(!e.target.checked)}
                                />
                            }
                            label="Private chat"
                        />
                        {multiSelect && (
                            <TextField
                                label="Chat name"
                                required
                                onChange={(e) => setChatName(e.target.value)}
                            />
                        )}
                        <div className="participant-selection-popup__SearchBar">
                            <SearchBar onChange={setSearch} />
                        </div>
                        <div className="participant-selection-popup__UserList">
                            <UserList
                                users={users}
                                checked={checked}
                                handleToggle={handleToggle}
                                ref={lastElementRef}
                                multiSelect={multiSelect}
                            />
                        </div>
                        <div className="participant-selection-popup__Confirmation">
                            <Button
                                variant="contained"
                                onClick={handleClose}
                                sx={{ marginRight: '8px' }}
                            >
                                Cancel
                            </Button>
                            <Button
                                variant="contained"
                                disabled={checked.length === 0 || isLoading}
                                onClick={handleOk}
                            >
                                Ready
                            </Button>
                        </div>
                    </div>
            }
        </Dialog>
    );
}