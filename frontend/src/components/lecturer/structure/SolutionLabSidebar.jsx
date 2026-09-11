import { BookOpen, Plus, Trash2 } from 'lucide-react';
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  useSidebar,
} from '../../ui/sidebar';
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from '../../ui/item';
import { Badge } from '../../ui/badge';
import { formatLabDeadlineMeta } from '../../../theme/statusClasses';

function labDescription(lab) {
  if (lab.studentVisible === false) return 'Hidden from students';
  if (lab.releaseDate) return `Releases ${String(lab.releaseDate).slice(0, 10)}`;
  return formatLabDeadlineMeta(lab, { withUrgencyHint: false });
}

function labBadge(lab) {
  if (lab.studentVisible === false) {
    return { variant: 'destructive', label: 'Hidden' };
  }
  if (lab.releaseDate) {
    return { variant: 'warning', label: 'Scheduled' };
  }
  if (lab.urgencyState === 'EXPIRED') {
    return { variant: 'secondary', label: 'Expired' };
  }
  return null;
}

export default function SolutionLabSidebar({
  labs = [],
  selectedLabId = null,
  onSelectLab = () => {},
  onAddLab = () => {},
  onDeleteLab = () => {},
}) {
  const { isMobile, setOpenMobile } = useSidebar();

  function handleSelect(labId) {
    onSelectLab(labId);
    if (isMobile) setOpenMobile(false);
  }

  return (
    <Sidebar>
      <SidebarHeader>
        <div className="flex items-start justify-between gap-2 px-1 py-0.5">
          <div>
            <p className="text-sm font-semibold text-foreground">Labs</p>
            <p className="text-xs text-foreground-muted">Manage rubrics and deadlines</p>
          </div>
          <button
            type="button"
            onClick={onAddLab}
            className="rounded-md p-1.5 text-foreground-muted transition-colors hover:bg-surface-secondary hover:text-primary"
            aria-label="Add lab"
          >
            <Plus className="h-4 w-4" />
          </button>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Assignments</SidebarGroupLabel>
          <SidebarGroupContent>
            {labs.length === 0 ? (
              <p className="px-2 py-6 text-center text-sm text-foreground-muted">
                No labs yet. Click + to create one.
              </p>
            ) : (
              <ItemGroup>
                {labs.map((lab) => {
                  const selected = String(lab.id) === String(selectedLabId);
                  const badge = labBadge(lab);
                  return (
                    <Item
                      key={lab.id}
                      as="button"
                      type="button"
                      size="sm"
                      variant={selected ? 'muted' : 'default'}
                      aria-current={selected ? 'true' : undefined}
                      onClick={() => handleSelect(lab.id)}
                      className={selected ? 'ring-1 ring-primary' : ''}
                    >
                      <ItemMedia variant="icon">
                        <BookOpen className="h-4 w-4 text-primary" />
                      </ItemMedia>
                      <ItemContent>
                        <ItemTitle className={selected ? 'text-primary-text' : undefined}>
                          {lab.name}
                        </ItemTitle>
                        <ItemDescription>{labDescription(lab)}</ItemDescription>
                      </ItemContent>
                      {(badge || selected) && (
                        <ItemActions className="flex items-center gap-1">
                          {badge && <Badge variant={badge.variant}>{badge.label}</Badge>}
                          {selected && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                onDeleteLab(lab.id);
                              }}
                              className="rounded p-1 text-foreground-muted hover:bg-error-bg hover:text-error"
                              aria-label={`Delete ${lab.name}`}
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          )}
                        </ItemActions>
                      )}
                    </Item>
                  );
                })}
              </ItemGroup>
            )}
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
